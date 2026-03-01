package com.consulta.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {

    private SecurityUtils() {
        // Construtor privado para evitar instanciação
    }

    public static UserDetails getUsuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return (UserDetails) auth.getPrincipal();
        }

        return null;
    }
}
