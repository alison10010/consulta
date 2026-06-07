package com.consulta.util.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.consulta.util.RelatorioEmail;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RelatorioDiarioSchedule {

    private final RelatorioEmail relatorioEmailService;

    // @Scheduled(cron = "0 0 17 * * *", zone = "America/Rio_Branco")
    @Scheduled(cron = "0 59 20 * * *", zone = "America/Rio_Branco")
    public void enviarRelatorioDiario() {
        relatorioEmailService.enviarRelatorioDiario();
    }
}