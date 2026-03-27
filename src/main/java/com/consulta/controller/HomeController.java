package com.consulta.controller;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.consulta.model.Usuario;
import com.consulta.repository.UsuarioRepository;

import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;

@Named
@Controller
@SessionScope
@Getter @Setter
public class HomeController implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	
	private String username;
    private String password;
    
	private String novaSenha;
	
	private String primeiroNome;
	
	Usuario usuarioLogado;
	
	private String localizacao;
	
	private boolean colaborador;
	
	private boolean paciente;
	
	private boolean admin;
	
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@GetMapping("/login")
	public String login() {
		return "login/index"; 
	}
	
	@GetMapping("/")
	public String home()
	{	
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		// verifica se possui ROLE_COLABORADOR
		colaborador = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COLABORADOR"));
		
		paciente = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_NORMAL"));
		
		admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
	    		
		String URL = "";
		
		if(colaborador) {
			paciente = false;
			admin = false;
			URL = "colaborador/home";
		}
		
		if(paciente) {
			colaborador = false;
			admin = false;
			URL = "paciente/servicos";
		}
		
		if(admin) {
			colaborador = false;
			paciente = false;
			URL = "admin/especialidade-analise";
		}
		
		String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
		
		try {
			if(usuarioLogado == null) {
				usuarioLogado = usuarioRepository.findByUsername(usuario);
			}
			String[] inicioNome = usuarioLogado.getNome().split(" ");
			primeiroNome = inicioNome[0];
									
		} catch (Exception e) {}
				
						
		return "redirect:/"+URL;
	}
	
	@GetMapping("/colaborador/home")
	public String index() {
		return "colaborador/home"; 
	}	
	
	public String clientIp() {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "";
        HttpServletRequest req = attrs.getRequest();
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
	
	// #=============
	
	@GetMapping("/recupera-senha")
    public String resetPassword() {
        return "livre/recuperacao-senha";
    }
}
