package com.consulta.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimitService rateLimitService;

    // Método auxiliar para obter o IP (o mesmo que estava no seu Controller)
    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Aplica o filtro SOMENTE ao endpoint de login
        if (request.getRequestURI().endsWith("/loginViaToken") || request.getRequestURI().endsWith("/login")) {
            
            String ip = clientIp(request);
            
            if (rateLimitService.isRateLimitExceeded(ip)) {
                // Interrompe e envia 429 Too Many Requests
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Muitas requisicoes (Limite de 10 por minuto). Tente novamente mais tarde.");
                return; // INTERROMPE A CADEIA
            }
        }
        
        // Continua para o próximo filtro (e eventualmente para o Controller)
        filterChain.doFilter(request, response);
    }
}