package com.consulta.security;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.consulta.Enum.AuthOutcome;
import com.consulta.model.LogsUsuario;
import com.consulta.model.Usuario;
import com.consulta.record.UsuarioDetails;
import com.consulta.repository.LogsUsuarioRepository;
import com.consulta.service.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired private LoginAttemptService loginAttemptService;
 
    @Autowired private LogsUsuarioRepository logsUsuarioRepository; 
    
    @Autowired private UserDetailsService userDetailsService;
    
    @Lazy
    @Autowired private PasswordEncoder passwordEncoder;
    
    // Construtor: Spring Boot fará o Autowired automaticamente aqui
    public CustomAuthenticationProvider(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    private String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest req = attrs.getRequest();
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    /**
     * Centraliza a criação e o salvamento do log no banco de dados.
     */
    private void registrarLog(String username, String ip, AuthOutcome outcome, String reason, String localizacao) {
        LogsUsuario log = new LogsUsuario();
        log.setUsername(username);
        log.setIp(ip);
        log.setOutcome(outcome);
        log.setLocalizacao(localizacao);
        
        // Obtém os contadores para registro
        LoginAttemptService.Counter counters = loginAttemptService.getAttemptCounters(username, ip);
        log.setAttempts(counters.attempts);
        // Calcula o restante baseado no contador e no MAX_ATTEMPTS estático
        log.setRemaining(Math.max(0, LoginAttemptService.MAX_ATTEMPTS - counters.attempts));
        
        log.setReason(reason);
        // Salva a entidade no banco
        logsUsuarioRepository.save(log);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());
        String ip = clientIp();
        
        // --- CÓDIGO PARA OBTER LATITUDE E LONGITUDE ---
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String latitude = null;
        String longitude = null;
        
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            // O valor é obtido pelo atributo 'name' do campo HTML, que é 'latitude' e 'longitude'
            latitude = req.getParameter("latitude");
            longitude = req.getParameter("longitude");
        }
        
        String localizacao = latitude+","+longitude;
        // --------------------------------------------------
 

        String reason; // Variável para armazenar a razão do log

        // 1) Bloqueio prévio
        if (loginAttemptService.isBlocked(username, ip)) {
            long sec = loginAttemptService.secondsToUnblock(username, ip);
            long min = Math.max(1, sec / 60);
            reason = "Bloqueio por Brute Force. Libera em ~"+ min +" minuto(s)";

            // REGISTRO DE LOG: LOCKED
            registrarLog(username, ip, AuthOutcome.LOCKED, reason, localizacao);
            throw new LockedException(reason);
        }

        try {
        	
        	// 2) Carrega o usuário
            UserDetails userDetails = userDetailsService.loadUserByUsername(username); // userDetails é um UsuarioDetails
            
            // CORREÇÃO: Cast para a classe wrapper
            UsuarioDetails usuarioDetails = (UsuarioDetails) userDetails; 
            
            // Obtém a entidade Usuario real
            Usuario usuario = usuarioDetails.getUsuario();
            
            // --- VERIFICAÇÃO DE SENHA ---
            if (!passwordEncoder.matches(password, usuario.getPassword())) { // Use usuario.getPassword()
                throw new BadCredentialsException("Credenciais inválidas: Senha incorreta."); 
            }
            // ------------------------------------------      	

            GrantedAuthority role = new SimpleGrantedAuthority("ROLE_" + usuario.getAcesso());

            return new UsernamePasswordAuthenticationToken(
                    usuarioDetails,
                    usuario.getPassword(),
                    Collections.singletonList(role)
            );
                        
        } catch (UsernameNotFoundException e) {
            // Apenas LANÇA A EXCEÇÃO. A contagem e o log serão feitos pelo Handler.
            throw new BadCredentialsException("Usuário não encontrado.", e);
            
        } catch (BadCredentialsException e) {
            throw e; // Propaga a exceção de senha incorreta
            
        } catch (AuthenticationException ae) {
            throw ae;
            
        } catch (Exception e) {
            // Erro interno (log de segurança é mantido, pois é uma falha de sistema)
            loginAttemptService.loginFailed(username, ip);
            reason = "Erro interno ao processar autenticação.";
            System.out.println(e);
            registrarLog(username, ip, AuthOutcome.INTERNAL_ERROR, reason, localizacao);
            throw new AuthenticationServiceException(reason, e);
        }
    }
    

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}