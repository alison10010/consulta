package com.consulta.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.consulta.model.Endereco;
import com.consulta.model.Usuario;
import com.consulta.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Component
public class UsuarioPadraoInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioPadraoInitializer(UsuarioRepository usuarioRepository,
                                    PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {

        final String USERNAME_PADRAO = "admin";

        Usuario existente = usuarioRepository.findByUsername(USERNAME_PADRAO);
        if (existente != null) {
            return;
        }

        // ===== USUÁRIO =====
        Usuario u = new Usuario();
        u.setNome("Administrador do Sistema");
        u.setUsername(USERNAME_PADRAO);
        u.setEmail("admin@sistema.local");
        u.setAcesso("ADMIN");
        u.setEmaiConfirmado(true);
        u.setTelefone("6832220000");
        u.setCelular("68999999999");
        u.setPossuiWpp(true);
        u.setTermoAceite(true);

        String senhaInicial = "admin@123";
        u.setPassword(passwordEncoder.encode(senhaInicial));

        // ===== ENDEREÇO =====
        Endereco e = new Endereco();
        e.setCep("69900-000");
        e.setEndereco("Rua Exemplo");
        e.setNumero("123");
        e.setBairro("Centro");
        e.setCidade("Rio Branco");
        e.setEstado("AC");
        e.setComplemento("Próximo à praça");
        e.setCoordenadas("-9.97499,-67.82430");

        // relacionamento bidirecional
        e.setUsuario(u);
        u.setEndereco(e);

        usuarioRepository.save(u);

        System.out.println("✔ Usuário padrão criado com endereço: admin / admin@123");
    }
}
