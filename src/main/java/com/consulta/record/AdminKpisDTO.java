package com.consulta.record;

import java.math.BigDecimal;

//KPIs gerais
public record AdminKpisDTO(
    long totalUsuarios,
    long totalAdmins,
    long totalColaboradores,
    long totalNormais,

    long totalHorariosPeriodo,
    long horariosLivresPeriodo,
    long horariosMarcadosPeriodo,
    long horariosPagosPeriodo,

    long pagamentosCriadosPeriodo,
    long pagamentosPagosPeriodo,
    BigDecimal totalPagoPeriodo
) {}

