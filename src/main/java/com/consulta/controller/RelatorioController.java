package com.consulta.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.SessionScope;

import com.consulta.Enum.StatusConsulta;
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
@SessionScope
@Getter
@Setter
public class RelatorioController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ZoneId RIO_BRANCO = ZoneId.of("America/Rio_Branco");

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private HorarioRepository horarioRepository;
    @Autowired private GuiaRepository guiaRepository;

    // range do p:datePicker (2 datas)
    private List<LocalDate> periodo = new ArrayList<>();

    private StatusConsulta statusConsultaFiltro;      // null = todos
    private Guia.StatusGuia statusGuiaFiltro;         // null = todos
    private Boolean somenteOcupados = true;

    private List<Horario> consultas = new ArrayList<>();
    private List<Guia> guias = new ArrayList<>();

    private int totalConsultas;
    private int totalConsultasOcupadas;
    private int totalConsultasLivres;
    private int totalGuias;

    @PostConstruct
    public void init() {
        aplicarPadraoUltimos30Dias();
        pesquisar();
    }

    public void aplicarPadraoUltimos30Dias() {
        LocalDate hoje = LocalDate.now(RIO_BRANCO);
        periodo.clear();
        periodo.add(hoje.minusDays(30));
        periodo.add(hoje);
    }

    public void limparFiltros() {
        aplicarPadraoUltimos30Dias();
        statusConsultaFiltro = null;
        statusGuiaFiltro = null;
        somenteOcupados = true;
        pesquisar();
    }

    public void pesquisar() {
        Usuario colab = getUsuarioLogado();
        if (colab == null || colab.getId() == null) {
            consultas = new ArrayList<>();
            guias = new ArrayList<>();
            zeraResumo();
            return;
        }

        LocalDate ini = getIni();
        LocalDate fim = getFim();

        // CONSULTAS (com fetch paciente)
        List<Horario> base = horarioRepository.listarRelatorioColaborador(colab, ini, fim);

        // filtros em memória (rápido e simples)
        List<Horario> filtrada = new ArrayList<>();
        int ocupadas = 0;
        for (Horario h : base) {
            if (h.getPaciente() != null) ocupadas++;

            if (Boolean.TRUE.equals(somenteOcupados) && h.getPaciente() == null) continue;
            if (statusConsultaFiltro != null && h.getStatus() != statusConsultaFiltro) continue;

            filtrada.add(h);
        }
        consultas = filtrada;

        // GUIAS (com fetch paciente/colaborador/horario)
        LocalDateTime dtIni = ini.atStartOfDay();
        LocalDateTime dtFim = LocalDateTime.of(fim, LocalTime.MAX);

        guias = guiaRepository.listarRelatorioColaborador(colab, dtIni, dtFim, statusGuiaFiltro);

        // RESUMO
        totalConsultas = base.size();
        totalConsultasOcupadas = ocupadas;
        totalConsultasLivres = totalConsultas - ocupadas;
        totalGuias = (guias != null ? guias.size() : 0);
    }

    private void zeraResumo() {
        totalConsultas = 0;
        totalConsultasOcupadas = 0;
        totalConsultasLivres = 0;
        totalGuias = 0;
    }

    private LocalDate getIni() {
        LocalDate hoje = LocalDate.now(RIO_BRANCO);
        if (periodo == null || periodo.size() < 2 || periodo.get(0) == null || periodo.get(1) == null) {
            return hoje.minusDays(30);
        }
        LocalDate a = periodo.get(0);
        LocalDate b = periodo.get(1);
        return b.isBefore(a) ? b : a;
    }

    private LocalDate getFim() {
        LocalDate hoje = LocalDate.now(RIO_BRANCO);
        if (periodo == null || periodo.size() < 2 || periodo.get(0) == null || periodo.get(1) == null) {
            return hoje;
        }
        LocalDate a = periodo.get(0);
        LocalDate b = periodo.get(1);
        return b.isBefore(a) ? a : b;
    }

    private Usuario getUsuarioLogado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username);
    }

    public StatusConsulta[] getStatusConsultaValues() {
        return StatusConsulta.values();
    }

    public Guia.StatusGuia[] getStatusGuiaValues() {
        return Guia.StatusGuia.values();
    }
}
