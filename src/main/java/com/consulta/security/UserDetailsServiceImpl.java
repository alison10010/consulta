package com.consulta.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.consulta.model.Usuario;
import com.consulta.record.UsuarioDetails;
import com.consulta.repository.UsuarioRepository;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	 @Autowired
	 private UsuarioRepository usuarioRepository;
	     
	 @Override
	 public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
	     Usuario u = usuarioRepository.findByUsername(username);
	     
	     if (u == null) {
	         throw new UsernameNotFoundException("Invalido");
	     }
	     
	     // Retorna o objeto DTO que implementa UserDetails
	     return new UsuarioDetails(u); 
	 }

}