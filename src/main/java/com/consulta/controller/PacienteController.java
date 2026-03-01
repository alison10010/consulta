package com.consulta.controller;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.annotation.SessionScope;

import com.consulta.Enum.StatusConsulta;
import com.consulta.model.Endereco;
import com.consulta.model.Horario;
import com.consulta.model.Pagamento;
import com.consulta.model.Usuario;
import com.consulta.repository.HorarioRepository;
import com.consulta.repository.PagamentoRepository;
import com.consulta.repository.UsuarioRepository;
import com.consulta.service.PagamentoService;
import com.consulta.util.Mensagens;
import com.consulta.util.Redirecionar;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;

@Named
@Controller
@SessionScope
@Getter
@Setter
public class PacienteController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @Autowired private UsuarioRepository usuarioRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    
    @Autowired private HorarioRepository horarioRepository;
    
    @Autowired private GuiaController guiaController;
    
    @Autowired private PagamentoRepository pagamentoRepository;
    
    private PagamentoService pagamentoService;
    
    private Pagamento pagamentoModal;
    
    private Pagamento pagamentoAtual;

    private Usuario colaboradorSelecionado;
    private List<Horario> horariosDisponiveis;
    private LocalDate dataSelecionada = LocalDate.now();
    
    private String especialidadeSelecionadaNome;
    private BigDecimal valorSelecionado;
    private BigDecimal valorComGuia;


    private Usuario usuario = new Usuario();
    
    private List<Horario> minhasConsultas;
    
    private Endereco enderecoModalObj;

    

    @PostConstruct
    public void init() {
    	carregarUsuarioLogado();
        carregarMinhasConsultas();
    }
    
    public PacienteController(PagamentoService pagamentoService,
            HorarioRepository horarioRepository,
            GuiaController guiaController) {
		this.pagamentoService = pagamentoService;
		this.horarioRepository = horarioRepository;
		this.guiaController = guiaController;
	}

    public void carregarUsuarioLogado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        usuario = usuarioRepository.findByUsername(username);

        // garante que o form não quebre
        if (usuario.getEndereco() == null) {
            Endereco e = new Endereco();
            e.setUsuario(usuario);
            usuario.setEndereco(e);
        }
    }
    
    public void salvar() {

        usuarioRepository.save(usuario);

        Mensagens.info("Registro salvo com sucesso!", "");
    }

    public void atualizaPerfil() {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            usuario = usuarioRepository.findByUsername(u);
        } catch (Exception e) {}
    }

    private Usuario getUsuarioLogado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username);
    }
    
    // #========= AGENDAR =========
    public void buscarHorarios() {
    	if (colaboradorSelecionado != null && dataSelecionada != null) {

            LocalTime agora = LocalTime.now();

            horariosDisponiveis = horarioRepository.buscarHorariosValidos(
                    colaboradorSelecionado,
                    dataSelecionada,
                    agora
            );
        }
    }

    public void agendar(Horario h) {
        Usuario paciente = getUsuarioLogado();

        // ✅ rebusca o horário (evita entidade solta/proxy)
        Horario horario = horarioRepository.findById(h.getId())
                .orElseThrow(() -> new IllegalArgumentException("Horário não encontrado"));

        // ✅ trava se já estiver ocupado
        if (!horario.isDisponivel() || horario.getPaciente() != null) {
            Mensagens.aviso("Ops!", "Este horário já foi ocupado. Selecione outro.");
            buscarHorarios();
            return;
        }

        horario.setPaciente(paciente);
        horario.setDisponivel(false);
        
        horario.setStatus(StatusConsulta.PROCESSANDO);
        horario.setAgendamentoPago(false);
        
        // expira em 20 min
        horario.setPixExpiraEm(LocalDateTime.now(ZoneId.of("America/Rio_Branco")).plusMinutes(20));
        
        horarioRepository.save(horario);

        // ✅ gera guia (controller já usa FETCH para evitar Lazy)
        guiaController.gerarGuiaDoAgendamento(horario.getId(), especialidadeSelecionadaNome, valorSelecionado, valorComGuia);

        // ✅ atualiza listas (opcional, mas útil se voltar)
        buscarHorarios();
        carregarMinhasConsultas();
        
        // cria checkout do PIX (R$20)
        pagamentoService.gerarPix(h.getId(), new BigDecimal("0.50"));

        Mensagens.info("Sucesso!", "Horário agendado para " + horario.getHora());

        // vai pra guia
        Redirecionar.irParaURL("paciente/consultas");
    }   
    
    // ===== PAGAMENTO =====
    public Pagamento getPagamentoModal() {
        return pagamentoModal;
    }
    
    // 	===== LIBERA HORARIO NOVAMENTE DEPOIS DE 20M =====
    @Scheduled(cron = "0 */1 * * * *", zone = "America/Rio_Branco") // Roda a cada 1 minuto
    @Transactional
    public void cleanup() {

        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Rio_Branco"));

        horarioRepository.liberarHorariosExpirados(agora);

    }

    
    // Ajuste para exibir o valor formatado no Modal
    public String getPagamentoValorReais() {
        // Usamos o pagamentoAtual que é o que o modal utiliza
        if (this.pagamentoAtual == null || this.pagamentoAtual.getValor() == null) {
            return "20,00"; 
        }
        
        // Formata o BigDecimal (ex: 20.00) para String brasileira (ex: 20,00)
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,.2f", this.pagamentoAtual.getValor());
    }

    public void abrirPagamentoPix(Long horarioId) {
        try {
            // 1. Tenta buscar um pagamento já existente para este horário
            Optional<Pagamento> pgExistente = pagamentoRepository.findByHorarioId(horarioId);
            
            if (pgExistente.isPresent()) {
                this.pagamentoAtual = pgExistente.get();
            } else {
                // 2. Se não existir, gera um novo via Service (passando os 20.00 em Reais)
                this.pagamentoAtual = pagamentoService.gerarPix(horarioId, new BigDecimal("20.00"));
            }            
            
            PrimeFaces.current().executeScript("PF('dlgPix').show()");
        } catch (Exception e) {
            e.printStackTrace();
            Mensagens.erro("Erro", "Não foi possível gerar o QR Code PIX.");
        }
    }

    public void verificarPagamento(Long horarioId) {
        Pagamento pg = pagamentoRepository.findByHorarioId(horarioId).orElse(null);
        
        if (pg != null && pg.getMpPaymentId() != null) {
            // Vai até o Mercado Pago conferir a situação real
            pagamentoService.verificarEAtualizarStatus(pg.getMpPaymentId());
            
            // Re-checa o status local após a atualização
            pg = pagamentoRepository.findByHorarioId(horarioId).get();
            if (pg.getStatus() == Pagamento.StatusPg.PAID) {
                Mensagens.info("Sucesso!", "Pagamento confirmado!");
                confirmarPagamentoAgendamento(horarioId);
                carregarMinhasConsultas();
                return;
            }
        }
        Mensagens.aviso("Pendente", "O pagamento ainda não foi detectado. Se já pagou, aguarde 30 segundos.");
    }    
    

    public Pagamento getPagamentoAtual() {
        return pagamentoAtual;
    }
    
    @Transactional
    public void confirmarPagamentoAgendamento(Long horarioId) {

        Horario h = horarioRepository.findById(horarioId)
            .orElseThrow(() -> new IllegalArgumentException("Horário não encontrado"));

        if (Boolean.TRUE.equals(h.getAgendamentoPago())) {
            return;
        }

        Pagamento pg = pagamentoRepository.findByHorarioId(horarioId)
            .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado para este horário"));

        if (pg.getStatus() != Pagamento.StatusPg.PAID) {
            throw new IllegalStateException("Pagamento ainda não confirmado (status=" + pg.getStatus() + ")");
        }

        h.setAgendamentoPago(true);
        h.setStatus(StatusConsulta.MARCADA);
        horarioRepository.save(h);
    }
    
    public void carregarMinhasConsultas() {
        Usuario p = getUsuarioLogado();
        minhasConsultas = horarioRepository.listarConsultasPacienteComColaborador(p);

    }
    
    public boolean isTemConsultaProcessando() {
        return minhasConsultas != null &&
               minhasConsultas.stream().anyMatch(h -> h.getStatus() == StatusConsulta.PROCESSANDO);
    }
    
    public Long pixExpiraEpochSeconds(Horario h) {
        if (h == null || h.getPixExpiraEm() == null) return null;

        // Converte LocalDateTime -> epoch usando o fuso de Rio Branco
        return h.getPixExpiraEm().atZone(ZoneId.of("America/Rio_Branco")).toEpochSecond();
    }

    public void cancelarConsulta(Long horarioId) {
        Usuario p = getUsuarioLogado();

        Horario h = horarioRepository.findByIdAndPaciente(horarioId, p)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));

        h.setPaciente(null);
        h.setDisponivel(true);
        horarioRepository.save(h);

        carregarMinhasConsultas();
        Mensagens.info("Cancelado!", "Sua consulta foi cancelada.");
    }
    
    
    public Endereco getEnderecoModalObj() {
        return enderecoModalObj;
    }

    public void abrirEndereco(Horario h) {
        enderecoModalObj = null;

        if (h == null || h.getColaborador() == null) return;

        enderecoModalObj = h.getColaborador().getEndereco();
    }


    // ===== Rotas =====
    @GetMapping("/paciente/perfil")
    public String perfil() {
        atualizaPerfil();
        return "paciente/perfil";
    }
    
    @GetMapping("/paciente/servicos")
    public String servico() {
        return "paciente/servicos";
    }
    
    @PostMapping("/paciente/agendamento")
    public String agendamento(
            @RequestParam("id") Long colaboradorId,
            @RequestParam("especialidade") String especialidade,
            @RequestParam("valor") BigDecimal valor,
            @RequestParam("valorComGuia") BigDecimal valorComGuia
    ) {
        this.colaboradorSelecionado =
            usuarioRepository.findById(colaboradorId).orElse(null);

        this.especialidadeSelecionadaNome = especialidade;
        this.valorSelecionado = valor;
        
        this.valorComGuia = valorComGuia;

        buscarHorarios();
        return "paciente/agendamento";
    }
    
    @GetMapping("/paciente/consultas")
    public String consulta() {
    	carregarMinhasConsultas();
        return "paciente/consultas";
    }
    
    @GetMapping("/paciente/alterar-senha")
    public String AlterarSenha() {        
    	return "colaborador/alterar-senha";
    }
}
