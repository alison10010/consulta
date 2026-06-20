package com.consulta.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.annotation.SessionScope;

import com.consulta.model.Endereco;
import com.consulta.model.Especialidade;
import com.consulta.model.Horario;
import com.consulta.model.PacienteConvidado;
import com.consulta.model.Termo;
import com.consulta.model.Usuario;
import com.consulta.model.UsuarioEspecialidade;
import com.consulta.repository.EspecialidadeRepository;
import com.consulta.repository.HorarioRepository;
import com.consulta.repository.PacienteConvidadoRepository;
import com.consulta.repository.UsuarioEspecialidadeRepository;
import com.consulta.repository.UsuarioRepository;
import com.consulta.service.FileStorageService;
import com.consulta.service.MunicipioService;
import com.consulta.util.EmailConfirmaCadastro;
import com.consulta.util.Mensagens;
import com.consulta.util.Redirecionar;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;

@Named
@Controller
@SessionScope
@Getter
@Setter
public class ColaboradorController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired private MunicipioService municipioService;
    
    @Autowired private FileStorageService fileService;

    @Autowired private UsuarioRepository usuarioRepository;

    @Autowired private HorarioRepository horarioRepository;

    @Autowired private EspecialidadeRepository especialidadeRepository; 
    
    @Autowired private PacienteConvidadoRepository pacienteConvidadoRepository;
    
    @Autowired private PasswordEncoder passwordEncoder;
    
    @Autowired private EmailConfirmaCadastro emailConfirmaCadastro;

    private Usuario usuario = new Usuario();
    
    private UploadedFile fileDiploma;
    private UploadedFile fileCarteira;
    private UploadedFile fileDiplomaVerso;
    private UploadedFile fileCarteiraVerso;
    
    private UploadedFile pathFotoPerfil;

    // lista pra montar o select
    private List<String> especialidadesDisponiveis;
    
    private List<Especialidade> todasEspecialidades;
    
    // IDs selecionados no formulário
    private List<Long> especialidadesSelecionadas = new ArrayList<>();

    // ====== UF / MUNICIPIO (selects) ======
    private String ufSelecionado;
    private String municipioSelecionado;
    private List<String> municipios = new ArrayList<>();
    
    
    // ====== ALTERAR SENHA  ======
    private String novaSenha;
    private String confirmarSenha;
    
    // ====== AGENDA DO DIA (COLABORADOR) ======
    private LocalDate dataAgenda = LocalDate.now();
    private List<Horario> agendaDoDia = new ArrayList<>();    
    private Usuario pacienteSelecionado;
    
    // ====== SPECIALIDADE/USUARIO
    @Autowired private UsuarioEspecialidadeRepository usuarioEspecialidadeRepository;

    // ADD UMA NOVA ESPECIALIDADE
    private Long especialidadeSelecionadaId;
    private BigDecimal valorSelecionado;
    private BigDecimal valorComGuia;
    private String conselho;
    private String registro;
    private UsuarioEspecialidade usuarioEspecialidade;
        
    private Integer maxVagasDia = 10;
    
    private PacienteConvidado pacienteConvidadoModal; 

    private List<UsuarioEspecialidade> especialidadesDoUsuario = new ArrayList<>();


    // ================= HORÁRIOS ===========

    // p:datePicker selectionMode="range" => lista com 2 datas (ini/fim)
    private List<LocalDate> periodo = new ArrayList<>();

    // valores: "MONDAY", "TUESDAY", ...
    private List<String> diasSemana = new ArrayList<>(Arrays.asList(
        "MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"
    ));

    // inputMask "99:99"
    private String horaInicio = "08:00";
    private String horaFim = "12:00";

    private Integer intervalo = 10;

    private Boolean excluirFds = true;

    @PostConstruct
    public void init() {
        especialidadesDisponiveis = usuarioRepository.listarEspecialidadesUsadas();
        carregarUsuarioLogado();
        syncEnderecoParaSelects();
            
        todasEspecialidades = especialidadeRepository.findAll(Sort.by("especialidade"));        
        
        buscarAgendaDoDia();
    }

    public void carregarUsuarioLogado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        usuario = usuarioRepository.findByUsername(username);
        especialidadesDoUsuario = usuarioEspecialidadeRepository.findByUsuarioId(usuario.getId());
        // garante que o form não quebre
        if (usuario.getEndereco() == null) {
            Endereco e = new Endereco();
            e.setUsuario(usuario);
            usuario.setEndereco(e);
        }
    }

    public void atualizaPerfil() {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            usuario = usuarioRepository.findByUsername(u);
        } catch (Exception e) {System.out.println(e);}
    }

    public void salvar() {
        try {
            // 1. Salva os dados básicos primeiro (ou garante que o ID existe)
            usuario = usuarioRepository.save(usuario);

            Mensagens.info("Registro e documentos salvos com sucesso!", "");

        } catch (Exception e) {            
            Mensagens.erro("Erro ao salvar registro ou arquivos. ", "");
        }
    }
       
    
    // #===== INICIO DE UPLOAD DOS DOCUMENTOS
    
    @Transactional
    public void addEspecialidades() {

        if (especialidadeSelecionadaId == null) {
            Mensagens.aviso("Selecione uma especialidade.", "");
            return;
        }

        if (valorSelecionado == null || valorSelecionado.compareTo(BigDecimal.ZERO) <= 0) {
            Mensagens.aviso("Informe um valor válido.", "");
            return;
        }

        if (valorComGuia == null || valorComGuia.compareTo(BigDecimal.ZERO) <= 0) {
            Mensagens.aviso("Informe um valor com a guia.", "");
            return;
        }

        if (usuario == null || usuario.getId() == null) {
            Mensagens.erro("Usuário não carregado.", "");
            return;
        }

        if (usuarioEspecialidadeRepository.existsByUsuarioIdAndEspecialidadeId(usuario.getId(), especialidadeSelecionadaId)) {
            Mensagens.aviso("Você já adicionou essa especialidade.", "");
            return;
        }

        Especialidade esp = especialidadeRepository.findById(especialidadeSelecionadaId).orElse(null);

        if (esp == null) {
            Mensagens.aviso("Especialidade inválida.", "");
            return;
        }

        UsuarioEspecialidade ue = new UsuarioEspecialidade();
        ue.setUsuario(usuario);
        ue.setEspecialidade(esp);
        ue.setValor(valorSelecionado);
        ue.setValorComGuia(valorComGuia);
        ue.setConselho(conselho);
        ue.setRegistro(registro);
        ue.setStatus("PENDENTE_DOCS");

        try {

            String espNome = normalizaParaArquivo(esp.getEspecialidade());
            ue.setNomeArquivo(espNome);

            if (fileDiploma != null && fileDiploma.getSize() > 0) {
                String nomeSalvo = fileService.salvarDocumentoJSF(
                        usuario.getId(),
                        espNome + "_diploma_frente",
                        fileDiploma
                );
                ue.setPathDiplomaEspecialidade(nomeSalvo);
            }

            if (fileDiplomaVerso != null && fileDiplomaVerso.getSize() > 0) {
                String nomeSalvo = fileService.salvarDocumentoJSF(
                        usuario.getId(),
                        espNome + "_diploma_verso",
                        fileDiplomaVerso
                );
                ue.setPathDiplomaEspecialidadeVerso(nomeSalvo);
            }

            if (fileCarteira != null && fileCarteira.getSize() > 0) {
                String nomeSalvo = fileService.salvarDocumentoJSF(
                        usuario.getId(),
                        espNome + "_carteira_frente",
                        fileCarteira
                );
                ue.setPathCarteiraEspecialidade(nomeSalvo);
            }

            if (fileCarteiraVerso != null && fileCarteiraVerso.getSize() > 0) {
                String nomeSalvo = fileService.salvarDocumentoJSF(
                        usuario.getId(),
                        espNome + "_carteira_verso",
                        fileCarteiraVerso
                );
                ue.setPathCarteiraEspecialidadeVerso(nomeSalvo);
            }

            boolean temDiplomaFrente = ue.getPathDiplomaEspecialidade() != null;
            boolean temDiplomaVerso = ue.getPathDiplomaEspecialidadeVerso() != null;
            boolean temCarteiraFrente = ue.getPathCarteiraEspecialidade() != null;
            boolean temCarteiraVerso = ue.getPathCarteiraEspecialidadeVerso() != null;

            if (temDiplomaFrente && temDiplomaVerso && temCarteiraFrente && temCarteiraVerso) {
                ue.setStatus("EM_ANALISE");
            } else {
                ue.setStatus("PENDENTE_DOCS");
            }

        } catch (IOException ex) {
            Mensagens.erro("Erro ao salvar documentos: " + ex.getMessage(), "");
            return;

        } finally {
            fileDiploma = null;
            fileDiplomaVerso = null;
            fileCarteira = null;
            fileCarteiraVerso = null;
        }

        usuarioEspecialidadeRepository.save(ue);

        especialidadesDoUsuario = usuarioEspecialidadeRepository.findByUsuarioId(usuario.getId());

        especialidadeSelecionadaId = null;
        valorSelecionado = null;
        valorComGuia = null;
        conselho = null;
        registro = null;

        Mensagens.info("Especialidade adicionada!", "");
    }
    
    
    public void prepararEdicaoValores(UsuarioEspecialidade ue) {
        this.usuarioEspecialidade = ue;
    }
    
    @Transactional
    public void atualizarValoresEspecialidade() {
        if (usuarioEspecialidade == null) return;

        if (usuarioEspecialidade.getValor() == null || 
        		usuarioEspecialidade.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            Mensagens.aviso("Informe um valor original válido.", "");
            return;
        }

        try {

            usuarioEspecialidadeRepository.save(usuarioEspecialidade);
            
            Mensagens.info("Valores atualizados com sucesso!", "");

            especialidadesDoUsuario = usuarioEspecialidadeRepository.findByUsuarioId(usuario.getId());
            
        } catch (Exception e) {
            Mensagens.erro("Erro ao atualizar valores.", "");
        }
    }
    
    public void removerEspecialidade(UsuarioEspecialidade esp) {

        if (esp == null || esp.getId() == null) {
            return;
        }

        usuarioEspecialidadeRepository.deleteById(esp.getId());

        especialidadesDoUsuario = usuarioEspecialidadeRepository.findByUsuarioId(usuario.getId());
    }
    
    private String normalizaParaArquivo(String s) {
        if (s == null) return "sem_nome";
        return s.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_\\-]", ""); // tira acentos/símbolos
    }

    private String getExtensao(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return (idx >= 0) ? filename.substring(idx).toLowerCase() : "";
    }
    
    private void salvarUploadedFileNoDisco(Path pasta, String nomePadrao, UploadedFile arquivo) throws IOException {
        if (arquivo == null || arquivo.getSize() <= 0) return;

        Files.createDirectories(pasta);

        String ext = getExtensao(arquivo.getFileName());
        Path destino = pasta.resolve(nomePadrao + ext);

        try (var in = arquivo.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    
    // #===== FIM DE UPLOAD DOS DOCUMENTOS
    
    
    // #========= FOTO DE PERFIL - INICIO
    
    public void visualizarFotoPerfil() {
        try {

            String nomeArquivo = usuario.getPerfil();

            if (nomeArquivo == null || nomeArquivo.isEmpty()) {
                return;
            }

            byte[] arquivoBytes = fileService.buscarBytesArquivo(usuario.getId(), nomeArquivo);

            if (arquivoBytes != null) {

                FacesContext context = FacesContext.getCurrentInstance();
                ExternalContext external = context.getExternalContext();

                external.responseReset();

                String contentType = external.getMimeType(nomeArquivo);

                external.setResponseContentType(
                    contentType != null ? contentType : "image/png"
                );

                external.setResponseHeader(
                    "Content-Disposition",
                    "inline; filename=\"" + nomeArquivo + "\""
                );

                OutputStream out = external.getResponseOutputStream();
                out.write(arquivoBytes);

                context.responseComplete();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public StreamedContent getFotoPerfil() {
        try {
            if (usuario == null || usuario.getPerfil() == null || usuario.getPerfil().isBlank()) {
                return imagemPadrao();
            }

            byte[] bytes = fileService.buscarBytesArquivo(
                    usuario.getId(),
                    usuario.getPerfil()
            );

            if (bytes == null || bytes.length == 0) {
                return imagemPadrao();
            }

            return DefaultStreamedContent.builder()
                    .contentType("image/png")
                    .stream(() -> new ByteArrayInputStream(bytes))
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return imagemPadrao();
        }
    }
    
    private StreamedContent imagemPadrao() {
        return DefaultStreamedContent.builder()
                .contentType("image/png")
                .stream(() -> {
                    InputStream is = getClass()
                            .getResourceAsStream("/static/img/perfil.png");

                    if (is == null) {
                        return new ByteArrayInputStream(new byte[0]);
                    }

                    return is;
                })
                .build();
    }
    
    public void uploadFotoPerfil(FileUploadEvent event) {

        try {
            this.pathFotoPerfil = event.getFile();
            String imgPerfil = fileService.salvarDocumentoJSF(usuario.getId(), "perfil", pathFotoPerfil, "COLABORADOR");
            usuario.setPerfil(imgPerfil);

            usuarioRepository.save(usuario);

            Mensagens.info("Foto alterada!", "");

        } catch (Exception e) {
            e.printStackTrace();
            Mensagens.erro("Erro ao alterar foto", "");
        }
    }
    
    // 	#========= FOTO DE PERFIL - FIM
    
    

    public void salvarEndereco() {
        if (usuario != null && usuario.getEndereco() != null) {
            usuario.getEndereco().setEstado(toUpperTrim2(ufSelecionado));
            usuario.getEndereco().setCidade(trimToNull(municipioSelecionado));
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario uDb = usuarioRepository.findByUsername(username);

        if (uDb == null) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Usuário logado não encontrado.");
            return;
        }

        Endereco eDb = uDb.getEndereco();
        if (eDb == null) {
            eDb = new Endereco();
            eDb.setUsuario(uDb);
            uDb.setEndereco(eDb);
        }

        Endereco eForm = (usuario != null ? usuario.getEndereco() : null);
        if (eForm == null) {
            addMsg(FacesMessage.SEVERITY_WARN, "Preencha o endereço antes de salvar.");
            return;
        }

        String cepDigits = onlyDigits(eForm.getCep());
        if (cepDigits != null && !cepDigits.isEmpty() && cepDigits.length() != 8) {
            addMsg(FacesMessage.SEVERITY_WARN, "CEP inválido. Use 00000-000.");
            return;
        }

        eDb.setCep(trimToNull(eForm.getCep()));
        eDb.setEndereco(trimToNull(eForm.getEndereco()));
        eDb.setNumero(trimToNull(eForm.getNumero()));
        eDb.setBairro(trimToNull(eForm.getBairro()));
        eDb.setCidade(trimToNull(eForm.getCidade()));
        eDb.setEstado(toUpperTrim2(eForm.getEstado()));
        eDb.setComplemento(trimToNull(eForm.getComplemento()));
        eDb.setCoordenadas(trimToNull(eForm.getCoordenadas()));
        eDb.setUsuario(uDb);

        usuarioRepository.save(uDb);
        usuario = uDb;

        syncEnderecoParaSelects();

        Mensagens.info("Endereço atualizado com sucesso!", "");
        Redirecionar.irParaURL("colaborador/endereco");
    }

    // ===== Municipios =====

    public void onChangeUF() {
        if (usuario != null && usuario.getEndereco() != null) {
            usuario.getEndereco().setEstado(toUpperTrim2(ufSelecionado));
            usuario.getEndereco().setCidade(null);
        }

        municipios = new ArrayList<>(municipioService.listarMunicipiosPorUF(ufSelecionado));
        municipioSelecionado = null;
    }

    public void onChangeMunicipio() {
        if (usuario != null && usuario.getEndereco() != null) {
            usuario.getEndereco().setCidade(trimToNull(municipioSelecionado));
        }
    }

    public void aplicarCep(String uf, String cidade, String bairro, String logradouro) {
        if (usuario == null) usuario = new Usuario();
        if (usuario.getEndereco() == null) {
            Endereco e = new Endereco();
            e.setUsuario(usuario);
            usuario.setEndereco(e);
        }

        if (uf != null) usuario.getEndereco().setEstado(toUpperTrim2(uf));
        if (cidade != null) usuario.getEndereco().setCidade(trimToNull(cidade));
        if (bairro != null) usuario.getEndereco().setBairro(trimToNull(bairro));
        if (logradouro != null) usuario.getEndereco().setEndereco(trimToNull(logradouro));

        syncEnderecoParaSelects();
    }

    private void syncEnderecoParaSelects() {
        if (usuario == null || usuario.getEndereco() == null) return;

        String uf = toUpperTrim2(usuario.getEndereco().getEstado());
        this.ufSelecionado = uf;

        this.municipios = new ArrayList<>(municipioService.listarMunicipiosPorUF(uf));

        String cid = trimToNull(usuario.getEndereco().getCidade());
        this.municipioSelecionado = cid;
    }

    public List<String> getEstados() {
        return Arrays.asList(
            "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA",
            "MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
            "RS","RO","RR","SC","SP","SE","TO"
        );
    }

    // ======================= GERAR HORÁRIOS =======================
    public void gerarPorPeriodo() {

        Usuario colaborador = getUsuarioLogado();
        if (colaborador == null || colaborador.getAcesso() == null
            || !"COLABORADOR".equalsIgnoreCase(colaborador.getAcesso())) {
            Mensagens.aviso("Apenas COLABORADOR pode gerar horários.", "");
            return;
        }

        if (periodo == null || periodo.size() < 2 || periodo.get(0) == null || periodo.get(1) == null) {
            Mensagens.aviso("Selecione um período (data inicial e final).", "");
            return;
        }

        if (diasSemana == null || diasSemana.isEmpty()) {
            Mensagens.aviso("Selecione pelo menos um dia da semana.", "");
            return;
        }

        if (maxVagasDia == null || maxVagasDia < 1) {
            Mensagens.aviso("Informe o limite máximo de vagas por dia.", "");
            return;
        }

        String hIniStr = trimToNull(horaInicio);
        String hFimStr = trimToNull(horaFim);

        if (hIniStr == null || hFimStr == null) {
            Mensagens.aviso("Preencha hora inicial e hora final.", "");
            return;
        }

        LocalDate ini = periodo.get(0);
        LocalDate fim = periodo.get(1);

        if (fim.isBefore(ini)) {
            LocalDate tmp = ini;
            ini = fim;
            fim = tmp;
        }

        LocalTime hIni;
        LocalTime hFim;

        try {
            hIni = LocalTime.parse(hIniStr);
            hFim = LocalTime.parse(hFimStr);
        } catch (Exception e) {
            Mensagens.aviso("Hora inválida. Use formato HH:mm (ex.: 08:00).", "");
            return;
        }

        if (!hFim.isAfter(hIni)) {
            Mensagens.aviso("Hora final deve ser maior que a inicial.", "");
            return;
        }

        int gerados = 0;
        LocalDate d = ini;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        String janelaStr = hIni.format(fmt) + " às " + hFim.format(fmt);

        while (!d.isAfter(fim)) {

            DayOfWeek dow = d.getDayOfWeek();
            boolean ehFds = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;

            if (Boolean.TRUE.equals(excluirFds) && ehFds) {
                d = d.plusDays(1);
                continue;
            }

            if (!diasSemana.contains(dow.name())) {
                d = d.plusDays(1);
                continue;
            }

            long totalNoDia = horarioRepository.countByColaboradorAndData(colaborador, d);

            if (totalNoDia >= maxVagasDia) {
                d = d.plusDays(1);
                continue;
            }

            int vagasNoDia = (int) totalNoDia;

            List<LocalTime> horariosDoDia = new ArrayList<>();

            if (maxVagasDia == 1) {
                horariosDoDia.add(hFim);
            } else {
                long totalSegundos = Duration.between(hIni, hFim).getSeconds();
                long passoSegundos = totalSegundos / (maxVagasDia - 1);

                for (int i = 0; i < maxVagasDia; i++) {

                    LocalTime horario;

                    if (i == maxVagasDia - 1) {
                        horario = hFim;
                    } else {
                        horario = hIni.plusSeconds(passoSegundos * i);
                    }

                    horariosDoDia.add(horario);
                }
            }

            for (LocalTime t : horariosDoDia) {

                if (vagasNoDia >= maxVagasDia) {
                    break;
                }

                if (horarioRepository.existsByColaboradorAndDataAndHora(colaborador, d, t)) {
                    continue;
                }

                Horario h = new Horario();
                h.setColaborador(colaborador);
                h.setData(d);
                h.setHora(t);
                h.setDisponivel(true);
                h.setPaciente(null);
                h.setVaga(janelaStr);

                horarioRepository.save(h);

                gerados++;
                vagasNoDia++;
            }

            d = d.plusDays(1);
        }

        Mensagens.info("Horários gerados: " + gerados, "");
    }
    
    // ======================= AGENDA DO DIA (COLABORADOR) =======================
    
    public void buscarAgendaDoDia() {
        Usuario colab = getUsuarioLogado();
        if (dataAgenda == null) dataAgenda = LocalDate.now();

        agendaDoDia = horarioRepository.listarAgendaDiaComPaciente(colab, dataAgenda);
    }

    public void hoje() {
        dataAgenda = LocalDate.now();
        buscarAgendaDoDia();
    }
    
    public Usuario getPacienteSelecionado() {
        return pacienteSelecionado;
    }

    public void abrirPaciente(Usuario p, Horario h) {
        this.pacienteSelecionado = p;        
        if(h.getPacienteConvidado() != null) {
        	this.pacienteConvidadoModal = pacienteConvidadoRepository.findById(h.getPacienteConvidado().getId()).orElse(null);
        }else {
        	this.pacienteConvidadoModal = null;
        }
    }

    public void removerHorario(Long horarioId) {
        Usuario colab = getUsuarioLogado();

        if (horarioId == null) {
            Mensagens.aviso("Horário inválido.", "");
            return;
        }

        // garante que é do colaborador
        var opt = horarioRepository.findByIdAndColaborador(horarioId, colab);
        if (opt.isEmpty()) {
            Mensagens.aviso("Horário não encontrado.", "");
            buscarAgendaDoDia();
            return;
        }

        Horario h = opt.get();

        // NÃO REMOVE SE ESTIVER OCUPADO
        if (!h.isDisponivel() || h.getPaciente() != null) {
            Mensagens.aviso("Não é possível remover: horário OCUPADO.", "");
            buscarAgendaDoDia();
            return;
        }

        // delete seguro (só apaga se ainda estiver LIVRE)
        int apagados = horarioRepository.deletarHorarioLivreDoColaborador(horarioId, colab);

        if (apagados > 0) {
            Mensagens.info("Horário removido!", "");
        } else {
            Mensagens.aviso("Não foi possível remover (talvez tenha sido ocupado).", "");
        }

        buscarAgendaDoDia();
    }
    
    public void visualizarDocumentoEspecialidade(UsuarioEspecialidade ue, String tipo) {
        try {
            String nomeArquivoNoBanco = null;

            if ("diploma".equals(tipo)) nomeArquivoNoBanco = ue.getPathDiplomaEspecialidade();
            if ("carteira".equals(tipo)) nomeArquivoNoBanco = ue.getPathCarteiraEspecialidade();
            
            if ("diploma_verso".equals(tipo)) nomeArquivoNoBanco = ue.getPathDiplomaEspecialidadeVerso();
            if ("carteira_verso".equals(tipo)) nomeArquivoNoBanco = ue.getPathCarteiraEspecialidadeVerso();

            if (nomeArquivoNoBanco == null || nomeArquivoNoBanco.isEmpty()) {
                Mensagens.aviso("Documento não anexado.", "");
                return;
            }

            // Chama o service passando o ID do usuário (2 no seu exemplo) e o nome exato (alergia_e_imunologia_diploma.png)
            byte[] arquivoBytes = fileService.buscarBytesArquivo(ue.getUsuario().getId(), nomeArquivoNoBanco);

            if (arquivoBytes != null) {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                ExternalContext externalContext = facesContext.getExternalContext();

                externalContext.responseReset();
                
                // O getMimeType vai identificar corretamente se é PNG ou PDF pelo nome
                String contentType = externalContext.getMimeType(nomeArquivoNoBanco);
                externalContext.setResponseContentType(contentType != null ? contentType : "application/octet-stream");
                
                externalContext.setResponseHeader("Content-Disposition", "inline; filename=\"" + nomeArquivoNoBanco + "\"");

                OutputStream outputStream = externalContext.getResponseOutputStream();
                outputStream.write(arquivoBytes);
                facesContext.responseComplete();
            } else {
                Mensagens.erro("Arquivo não encontrado no servidor: " + nomeArquivoNoBanco, "");
            }
        } catch (IOException e) {
            Mensagens.erro("Erro ao processar arquivo: " + e.getMessage(), "");
        }
    }
    
    // ===== EMAIL =====
    public void reenviarEmailConfirmacao() {
    	// envia Mensagem Assincrona sem esperar pela confirmação do envio do e-mail
    	String mail = usuario.getEmail();
        new Thread(() -> {
        	 emailConfirmaCadastro.enviaMensagem(mail, usuario.getHash());
        }).start();
        
        Mensagens.info("Enviado.", "");
    }
    
 	@GetMapping("/api/usuario/confirmar-cadastro")
 	public String confirmaCadastro(@RequestParam String hash) {

 		Usuario usuario = usuarioRepository.findByHash(hash);

 	    if (usuario == null) {
 	        return "redirect:/api/usuario/confirma?status=HASH_INVALIDO";
 	    }

 	    if (usuario.isEmaiConfirmado()) { // se já é boolean 
 	        return "redirect:/api/usuario/confirma?status=JA_CONFIRMADO";
 	    }

 	    try {
 	        usuario.setEmaiConfirmado(true);
 	        usuarioRepository.save(usuario);
 	        return "redirect:/api/usuario/confirma?status=SUCESSO";
 	    } catch (Exception e) {
 	        return "redirect:/api/usuario/confirma?status=FALHA";
 	    }
 	}
    
    // Página que renderiza o XHTML
 	@GetMapping("/api/usuario/confirma")
 	public String pageConfirmacao() {
 	    return "livre/confirmacao-cadastro";
 	}
 	
 	public void alteraSenha() {

 	    if (novaSenha == null || confirmarSenha == null ||
 	        novaSenha.isBlank() || confirmarSenha.isBlank()) {

 	        addMsg(FacesMessage.SEVERITY_WARN,
 	               "Preencha a nova senha e a confirmação.");
 	        return;
 	    }

 	    if (!novaSenha.equals(confirmarSenha)) {
 	        addMsg(FacesMessage.SEVERITY_ERROR,
 	               "A senha e a confirmação não conferem.");
 	        return;
 	    }

 	    if (novaSenha.length() < 6) {
 	        addMsg(FacesMessage.SEVERITY_WARN,
 	               "A senha deve ter no mínimo 6 caracteres.");
 	        return;
 	    }

 	    Usuario user = getUsuarioLogado();

 	    // 🔐 CRIPTOGRAFAR SEMPRE
 	    user.setPassword(passwordEncoder.encode(novaSenha));

 	    usuarioRepository.save(user);

 	    // limpar campos
 	    novaSenha = null;
 	    confirmarSenha = null;

 	    addMsg(FacesMessage.SEVERITY_INFO,
 	           "Senha alterada com sucesso.");
 	}

    
    // ===== helpers =====
 	
 	private Usuario getUsuarioLogado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username);
    }


    private void addMsg(FacesMessage.Severity sev, String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, msg, null));
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String onlyDigits(String s) {
        if (s == null) return null;
        return s.replaceAll("\\D", "");
    }

    private String toUpperTrim2(String uf) {
        uf = trimToNull(uf);
        return uf == null ? null : uf.toUpperCase();
    }

    // ===== Rotas =====

    @GetMapping("/colaborador/endereco")
    public String endereco() {
        return "colaborador/endereco";
    }

    @GetMapping("/colaborador/perfil")
    public String perfil() {
        atualizaPerfil();
        return "colaborador/perfil";
    }

    @GetMapping("/colaborador/horarios")
    public String horario() {
        atualizaPerfil();
        return "colaborador/horarios";
    }
    
    @GetMapping("/colaborador/agenda")
    public String agenda() {
        return "colaborador/agenda";
    }
    
    @GetMapping("/colaborador/alterar-senha")
    public String AlterarSenha() {
        return "colaborador/alterar-senha";
    }    
    
    @GetMapping("/colaborador/especialidade")
    public String especialidade() {
    	especialidadesDoUsuario = usuarioEspecialidadeRepository.findByUsuarioId(usuario.getId());
        return "colaborador/especialidade";
    }
    
    // ===== rota =====
    @GetMapping("/colaborador/relatorio")
    public String pageRelatorioColaborador() {
        return "colaborador/relatorio-colaborador";
    }
    
}
