package com.consulta.controller;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.consulta.record.AdminKpisDTO;
import com.consulta.record.EspecialidadeQtdDTO;
import com.consulta.repository.RelatorioRepository;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

@Named
@Controller
@ViewScoped
@Getter @Setter
public class AdminRelatorioController implements Serializable {

    private static final long serialVersionUID = 1L;

    private final RelatorioRepository relatorioRepository;

    // ===== Período =====
    private LocalDate ini;
    private LocalDate fim;

    // ===== Saída =====
    private AdminKpisDTO kpis;
    private List<EspecialidadeQtdDTO> especialidadesRanking = new ArrayList<>();

    // UI flag (opcional)
    private boolean carregando;

    public AdminRelatorioController(RelatorioRepository relatorioRepository) {
        this.relatorioRepository = relatorioRepository;
    }

    @PostConstruct
    public void init() {
        // padrão: mês atual
        ini = LocalDate.now().withDayOfMonth(1);
        fim = LocalDate.now();
        recarregarResumo();
    }

    public void recarregarResumo() {
        carregando = true;

        // normaliza período
        if (ini == null) ini = LocalDate.now().withDayOfMonth(1);
        if (fim == null) fim = LocalDate.now();
        if (fim.isBefore(ini)) {
            LocalDate tmp = ini; ini = fim; fim = tmp;
        }

        // datetime para pagamentos
        LocalDateTime iniDt = ini.atStartOfDay();
        LocalDateTime fimDt = fim.plusDays(1).atStartOfDay().minusNanos(1);

        // 1) KPIs (1 query)
        kpis = relatorioRepository.buscarKpis(ini, fim, iniDt, fimDt);

        // 2) Ranking especialidades (1 query)
        especialidadesRanking = relatorioRepository.rankingEspecialidades();

        carregando = false;
    }

    // ===== Helpers (pra EL ficar limpa e sem NPE) =====
    public long getTotalUsuarios() { return kpis == null ? 0 : kpis.totalUsuarios(); }
    public long getTotalAdmins() { return kpis == null ? 0 : kpis.totalAdmins(); }
    public long getTotalColaboradores() { return kpis == null ? 0 : kpis.totalColaboradores(); }
    public long getTotalNormais() { return kpis == null ? 0 : kpis.totalNormais(); }

    public long getTotalHorariosPeriodo() { return kpis == null ? 0 : kpis.totalHorariosPeriodo(); }
    public long getHorariosLivresPeriodo() { return kpis == null ? 0 : kpis.horariosLivresPeriodo(); }
    public long getHorariosMarcadosPeriodo() { return kpis == null ? 0 : kpis.horariosMarcadosPeriodo(); }
    public long getHorariosPagosPeriodo() { return kpis == null ? 0 : kpis.horariosPagosPeriodo(); }

    public long getPagamentosCriadosPeriodo() { return kpis == null ? 0 : kpis.pagamentosCriadosPeriodo(); }
    public long getPagamentosPagosPeriodo() { return kpis == null ? 0 : kpis.pagamentosPagosPeriodo(); }
    
    public long getTotalHorariosLivresPeriodo() { return kpis == null ? 0 : kpis.horariosLivresPeriodo(); }
    public long getTotalHorariosMarcadosPeriodo() { return kpis == null ? 0 : kpis.horariosMarcadosPeriodo(); }
    public long getTotalHorariosPagosPeriodo() { return kpis == null ? 0 : kpis.horariosPagosPeriodo(); }

    public BigDecimal getTotalPagoPeriodo() {
        return (kpis == null || kpis.totalPagoPeriodo() == null)
                ? BigDecimal.ZERO
                : kpis.totalPagoPeriodo();
    }

    // ===== Helpers extras úteis no XHTML =====
    public int getQtdEspecialidadesDiferentes() {
        return especialidadesRanking == null ? 0 : especialidadesRanking.size();
    }

    public boolean isSemDados() {
        return kpis == null;
    }
    
    public long getTotalPagamentosPeriodo() {
        return kpis == null ? 0 : kpis.pagamentosCriadosPeriodo();
    }

    public long getTotalPagamentosPagosPeriodo() {
        return kpis == null ? 0 : kpis.pagamentosPagosPeriodo();
    }
    
    @GetMapping("/admin/relatorio-colaborador")
    public String relatorio() {
        return "admin/relatorio-colaborador";
    }

}
