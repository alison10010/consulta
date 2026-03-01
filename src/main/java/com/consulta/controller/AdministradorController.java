package com.consulta.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.consulta.model.UsuarioEspecialidade;
import com.consulta.repository.UsuarioEspecialidadeRepository;
import com.consulta.repository.UsuarioRepository;
import com.consulta.util.Mensagens;
import com.consulta.util.VerificacaoDocsEmail;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

@Named
@Controller
@SessionScoped
@Getter @Setter
public class AdministradorController implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UsuarioEspecialidadeRepository usuarioEspecialidadeRepository;
    
    @Autowired private UsuarioRepository usuarioRepository;
    
    @Autowired private VerificacaoDocsEmail verificacaoDocsEmail;

    // ==== FILA ADMIN ====
    private List<UsuarioEspecialidade> fila = new ArrayList<>();
    private UsuarioEspecialidade selecionado;

    private String filtroStatus = "EM_ANALISE"; // EM_ANALISE | PENDENTE_DOCS | TODOS
    private String q; // busca texto
    private String motivoRecusa;

    public AdministradorController(UsuarioEspecialidadeRepository usuarioEspecialidadeRepository) {
        this.usuarioEspecialidadeRepository = usuarioEspecialidadeRepository;
    }

    @PostConstruct
    public void init() {
        recarregarFila();
    }

    public void recarregarFila() {
        List<String> sts = "TODOS".equalsIgnoreCase(filtroStatus)
                ? List.of("EM_ANALISE", "PENDENTE_DOCS")
                : List.of(filtroStatus);

        if (q != null && !q.trim().isEmpty()) {
            fila = usuarioEspecialidadeRepository.buscarFila(sts, q.trim());
        } else {
            fila = usuarioEspecialidadeRepository.findByStatusInOrderByCreatedAtDesc(sts);
        }
    }

    public void selecionar(UsuarioEspecialidade ue) {
        this.selecionado = ue;
        this.motivoRecusa = null;
    }

    // ====== AÇÕES SEM DUPLICAR actionListener ======
    public void aprovar(UsuarioEspecialidade ue) {
        selecionar(ue);
        aprovarSelecionado();
        Mensagens.info("Aprovado com sucesso!", "");
    }

    public void abrirRecusa(UsuarioEspecialidade ue) {
        selecionar(ue);
    }

    public void aprovarSelecionado() {
        if (selecionado == null) return;
        selecionado.setStatus("APROVADO");
        usuarioEspecialidadeRepository.save(selecionado);
        
        // Envio do e-mail
        verificacaoDocsEmail.enviaEmailResultadoAnalise(
            selecionado.getUsuario().getEmail(), 
            selecionado.getUsuario().getNome(), 
            selecionado.getEspecialidade().getEspecialidade(), 
            "APROVADO", 
            null
        );
        
        recarregarFila();
    }

    public void recusarSelecionado() {
        if (selecionado == null) return;
        selecionado.setStatus("RECUSADO");
        // selecionado.setMotivoRecusa(motivoRecusa); 
        usuarioEspecialidadeRepository.save(selecionado);
        
        // Envio do e-mail com o motivo informado no dialog
        verificacaoDocsEmail.enviaEmailResultadoAnalise(
            selecionado.getUsuario().getEmail(), 
            selecionado.getUsuario().getNome(), 
            selecionado.getEspecialidade().getEspecialidade(), 
            "RECUSADO", 
            this.motivoRecusa 
        );
        
        Mensagens.aviso("Recusado com sucesso!", "");
        
        recarregarFila();
    }

    @GetMapping("/admin/alterar-senha")
    public String AlterarSenha() {
        return "colaborador/alterar-senha";
    }   

    @GetMapping("/admin/especialidade-analise")
    public String especialidadeAnalise() {
        recarregarFila();
        return "admin/especialidade-analise";
    }
}