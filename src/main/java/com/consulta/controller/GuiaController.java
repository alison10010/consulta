package com.consulta.controller;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.consulta.model.Endereco;
import com.consulta.model.Guia;
import com.consulta.model.Horario;
import com.consulta.model.Usuario;
import com.consulta.repository.GuiaRepository;
import com.consulta.repository.HorarioRepository;
import com.consulta.repository.UsuarioRepository;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

@Named
@Controller
@Scope("session")
@Getter @Setter
public class GuiaController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired private GuiaRepository guiaRepository;
    @Autowired private HorarioRepository horarioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Guia guiaAtual;

    private static final SecureRandom RAND = new SecureRandom();
    private static final String ALPH = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem 0/O/1/I
    
    // # ====== VALIDACAO DE QRCODE
    private String codigoParam;   // vem da URL ?codigo=
    private String codigoBusca;   // digitado no input
    private String msgErro;
    private boolean expirada;
    
    @Value("${app.urlSite}")
    private String urlSite;

    @PostConstruct
    public void init() {
        guiaAtual = null;
    }

    public void gerarGuiaDoAgendamento(Long horarioId, String esp, BigDecimal valor, BigDecimal valorComGuia) {
        Usuario paciente = getUsuarioLogadoEntity();

        // ✅ Busca com FETCH (colaborador + endereco + paciente)
        Horario h = horarioRepository.buscarHorarioParaGuia(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horário não encontrado"));

        if (h.getPaciente() == null || !h.getPaciente().getId().equals(paciente.getId())) {
            throw new IllegalStateException("Este horário não pertence ao paciente logado.");
        }

        // se já existe guia para este horário, abre com FETCH
        guiaRepository.findByHorarioId(horarioId).ifPresent(existing -> {
            guiaAtual = guiaRepository.buscarGuiaFetch(existing.getId()).orElse(existing);
        });

        if (guiaAtual != null) return;

        Usuario colaborador = h.getColaborador();
        Endereco e = (colaborador != null) ? colaborador.getEndereco() : null;

        // ====== Regras de desconto (exemplo) ======
        
        BigDecimal valorOriginal = valor; 
        BigDecimal valorUsandoGuia = valorComGuia;

        // ✅ Aqui calculamos o percentual real baseado nos dois valores recebidos
        BigDecimal percentual = calcularPercentualDesconto(valorOriginal, valorUsandoGuia);

        String codigo = gerarCodigoUnico(10);

        Guia guia = Guia.builder()
                .codigo(codigo)
                .especialidade(esp)
                .emitidaEm(LocalDateTime.now())
                .validade(
                	    h.getData()
                	     .atTime(h.getHora())
                	     .plusMinutes(40)
                	     .toLocalDate()
                	)

                .percentualDesconto(percentual)
                .valorOriginal(valorOriginal)
                .valorComDesconto(valorUsandoGuia)
                .paciente(paciente)
                .colaborador(colaborador)
                .horario(h)
                .endereco(e != null ? e.getEndereco() : null)
                .numero(e != null ? e.getNumero() : null)
                .bairro(e != null ? e.getBairro() : null)
                .cidade(e != null ? e.getCidade() : null)
                .estado(e != null ? e.getEstado() : null)
                .cep(e != null ? e.getCep() : null)
                .status(Guia.StatusGuia.GERADA)
                .urlValidacao(urlSite+"/consulta/view/livre/verifica-guia.xhtml?codigo=" + codigo)
                .build();

        Guia salva = guiaRepository.save(guia);

        // ✅ Recarrega com FETCH (pra guia.xhtml ler colaborador/paciente/horario sem Lazy)
        guiaAtual = guiaRepository.buscarGuiaFetch(salva.getId()).orElse(salva);
    }

    public void abrirGuiaPorCodigo(String codigo) {
        guiaAtual = guiaRepository.buscarGuiaPorCodigoFetch(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Guia não encontrada"));
    }

    public void limparGuia() {
        guiaAtual = null;
    }

    private BigDecimal calcularPercentualDesconto(BigDecimal original, BigDecimal comDesconto) {
        if (original == null || comDesconto == null || original.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // (valorComDesconto * 100) / valorOriginal
        BigDecimal razao = comDesconto.multiply(BigDecimal.valueOf(100))
                .divide(original, 2, RoundingMode.HALF_UP);

        // 100 - razao
        return BigDecimal.valueOf(100).subtract(razao);
    }

    private String gerarCodigoUnico(int len) {
        for (int tent = 0; tent < 50; tent++) {
            String c = gerarCodigo(len);
            if (!guiaRepository.existsByCodigo(c)) return c;
        }
        return gerarCodigo(len) + System.currentTimeMillis();
    }

    private String gerarCodigo(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPH.charAt(RAND.nextInt(ALPH.length())));
        }
        return sb.toString();
    }

    private Usuario getUsuarioLogadoEntity() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username);
    }
    
    public void abrirGuiaPorHorario(Long horarioId) {

        // padrão: guia vazia
        guiaAtual = null;

        if (horarioId == null) {
            return;
        }

        guiaRepository.findByHorarioId(horarioId).ifPresent(g -> {
            // tenta recarregar com FETCH (evita Lazy)
            guiaAtual = guiaRepository.buscarGuiaFetch(g.getId())
                    .orElse(g);
        });
    }

    @GetMapping("/visualizar/guia")
    public String guia() {
        return "paciente/guia";
    }
    
    
    // #=============== VALIDACAO DE QRCODE
    public void carregarPorCodigoParam() {
        msgErro = null;
        expirada = false;

        if (codigoParam == null || codigoParam.isBlank()) return;

        try {
            abrirGuiaPorCodigo(codigoParam.trim().toUpperCase());
            validarExpiracao();
        } catch (Exception e) {
            guiaAtual = null;
            msgErro = "Código inválido ou guia não encontrada.";
        }
    }

    public void buscarPorCodigoDigitado() {
        msgErro = null;
        expirada = false;

        if (codigoBusca == null || codigoBusca.isBlank()) {
            msgErro = "Informe um código para buscar.";
            guiaAtual = null;
            return;
        }

        try {
            abrirGuiaPorCodigo(codigoBusca.trim().toUpperCase());
            validarExpiracao();
        } catch (Exception e) {
            guiaAtual = null;
            msgErro = "Código inválido ou guia não encontrada.";
        }
    }

    private void validarExpiracao() {
        // Se sua validade é LocalDate:
        if (guiaAtual != null && guiaAtual.getValidade() != null) {
            expirada = guiaAtual.getValidade().isBefore(LocalDate.now());
        }
    }

    public boolean isExpirada() { return expirada; }
}
