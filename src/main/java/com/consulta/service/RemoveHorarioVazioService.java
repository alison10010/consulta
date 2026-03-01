package com.consulta.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.consulta.repository.HorarioRepository;

@Component
public class RemoveHorarioVazioService {

    @Autowired private HorarioRepository horarioRepository;

    private static final ZoneId RIO_BRANCO = ZoneId.of("America/Rio_Branco");

    // ✅ todo dia às 04:00
    @Scheduled(cron = "0 0 4 * * *", zone = "America/Rio_Branco")
    public void removeHorarios() {
        LocalDate hoje = LocalDate.now(RIO_BRANCO);
        LocalTime agora = LocalTime.now(RIO_BRANCO);

        horarioRepository.deleteHorariosVaziosVencidos(hoje, agora);

        // opcional: log
        // System.out.println("Horários vazios removidos: " + removidos);
    }
}

