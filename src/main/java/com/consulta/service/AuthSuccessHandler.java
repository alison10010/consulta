package com.consulta.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.consulta.Enum.AuthOutcome;
import com.consulta.controller.HomeController;
import com.consulta.model.LogsUsuario;
import com.consulta.repository.LogsUsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired private LoginAttemptService loginAttemptService;
    @Autowired private LogsUsuarioRepository logsUsuarioRepository;
    
    @Autowired private HomeController homeController;

    private String getParam(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }
    
    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
    
    private void registrarLog(String username, String ip, AuthOutcome outcome, String reason, String localizacao) {
        LogsUsuario log = new LogsUsuario();
        log.setUsername(username);
        log.setIp(ip);
        log.setOutcome(outcome);
        log.setReason(reason);
        log.setLocalizacao(localizacao);

        var counters = loginAttemptService.getAttemptCounters(username, ip);
        log.setAttempts(counters.attempts);
        log.setRemaining(Math.max(0, LoginAttemptService.MAX_ATTEMPTS - counters.attempts));

        logsUsuarioRepository.save(log);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse resp,
                                        Authentication auth) throws IOException, ServletException {
        String username = auth.getName();
        String ip = clientIp(req);

        // geoloc do formulário
        String lat = getParam(req, "latitude");
       	String lng = getParam(req, "longitude");
       	
       	String loc = lat+","+lng;
       	
       	homeController.setLocalizacao(loc);

        // zera tentativas
        loginAttemptService.loginSucceeded(username, ip);

        registrarLog(username, ip, AuthOutcome.SUCCESS, "Login bem-sucedido", loc);

        // redirecione para sua home
        resp.sendRedirect("/consulta/");
    }
}
