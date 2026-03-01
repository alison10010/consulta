package com.consulta.controller;

import java.security.SecureRandom;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.consulta.model.Usuario;
import com.consulta.repository.UsuarioRepository;
import com.consulta.util.EmailRecuperaSenha;

@RestController
@RequestMapping("/api/")
@CrossOrigin(origins = "*")
public class RecuperarSenhaController {
	
	@Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailRecuperaSenha emailRecuperaSenha;

    @PostMapping("/recuperar-senha")
    public ResponseEntity<?> recuperaSenha(@RequestParam String username) {

        if (username == null || username.isBlank()) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("ok", false, "mensagem", "Informe um e-mail válido."));
        }

        String email = username.trim().toLowerCase();
        Usuario user = usuarioRepository.findByUsername(email);

        // 🔒 Segurança: sempre responda como se tivesse dado certo
        if (user == null) {
            return ResponseEntity.ok(Map.of(
                "ok", true,
                "mensagem", "Se este e-mail estiver cadastrado, enviaremos uma senha provisória."
            ));
        }

        String senhaProvisoria = gerarSenhaProvisoria();

        user.setPassword(passwordEncoder.encode(senhaProvisoria));
        usuarioRepository.save(user);

        emailRecuperaSenha.enviaMensagem(email, senhaProvisoria);

        return ResponseEntity.ok(Map.of(
            "ok", true,
            "mensagem", "Se este e-mail estiver cadastrado, enviaremos uma senha provisória."
        ));
    }

    private String gerarSenhaProvisoria() {
        SecureRandom random = new SecureRandom();
        return String.format("%05d", random.nextInt(100_000));
    }
}