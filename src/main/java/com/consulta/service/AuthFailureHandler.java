package com.consulta.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.consulta.Enum.AuthOutcome;
import com.consulta.model.LogsUsuario;
import com.consulta.repository.LogsUsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthFailureHandler implements AuthenticationFailureHandler {

    @Autowired private LoginAttemptService loginAttemptService;
    @Autowired private LogsUsuarioRepository logsUsuarioRepository;

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
    public void onAuthenticationFailure(
        HttpServletRequest request, 
        HttpServletResponse response, 
        AuthenticationException exception) throws IOException, ServletException {
        
        // 1. Coleta dados da requisição
        String username = request.getParameter("username"); // Nome do campo do formulário
        String ip = clientIp(request);
        
        // 2. Coleta localização
        String latitude = request.getParameter("latitude");
        String longitude = request.getParameter("longitude");
        String localizacao = latitude+","+longitude;
        String reason;
        
        if(ip.startsWith("10.10.5.")) { // REDE ACREPREV
        	localizacao = "-9.9728,-67.8024";
        }

        // Verifica se a falha é por credenciais (o que deve ser contabilizado)
        if (exception instanceof BadCredentialsException || exception instanceof UsernameNotFoundException) {
            
            // LOGICAMENTE SÓ SERÁ EXECUTADO UMA VEZ
            loginAttemptService.loginFailed(username, ip); 
            
            reason = "Falha de credenciais: " + exception.getMessage();
            registrarLog(username, ip, AuthOutcome.FAILURE, reason, localizacao);
        }

        // 3. Redirecionamento Final
        // Checa se a tentativa de login atingiu o limite (e se tornou LOCKED)
        if (loginAttemptService.isBlocked(username, ip)) {
            // Redireciona para o erro de bloqueio
            response.sendRedirect("/consulta/login?error=locked");
        } else {
            // Redireciona para o erro padrão de falha
        	response.sendRedirect("/consulta/login?error=bad");
        }
    }
}