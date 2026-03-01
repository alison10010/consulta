package com.consulta.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.consulta.model.Usuario;
import com.consulta.record.Credencial;
import com.consulta.repository.UsuarioRepository;


@RestController
@RequestMapping
public class AutenticacaoController {
	
	
	List<Usuario> listaUsuario;

	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@PostMapping("/api/autenticar")
	public ResponseEntity<?> autenticarUsuario(@RequestBody Credencial credenciais) {
	    String username = credenciais.username();
	    String password = credenciais.password();

	    try {
	        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

	        // Verifica se a senha está correta
	        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Senha incorreta");
	        }

	        // OK: usuário e senha válidos
	        Usuario usuario = usuarioRepository.findByUsername(username);
	        return ResponseEntity.ok(usuario);

	    } catch (UsernameNotFoundException e) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado");
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro: " + e.getMessage());
	    }
	}


}
