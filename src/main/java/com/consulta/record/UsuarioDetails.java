package com.consulta.record;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.consulta.model.Usuario;

public class UsuarioDetails implements UserDetails {

    private final Usuario usuario; // A entidade que está sendo embrulhada

    public UsuarioDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    // Método para acessar a entidade Usuario (necessário para o cast no Provider)
    public Usuario getUsuario() {
        return usuario;
    }

    // --- MÉTODOS UserDetails ---
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        String acesso = usuario.getAcesso();
        if (acesso == null || acesso.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String role = acesso.trim().toUpperCase();
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        return List.of(new SimpleGrantedAuthority(role));
    }


    @Override
    public String getPassword() {
        return this.usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return this.usuario.getUsername();
    }
    
    // Implementações de status (ajuste conforme a lógica real do seu sistema)
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
