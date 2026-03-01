package com.consulta.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.consulta.record.Credencial;

@RestController
@RequestMapping("/jwt/")
public class AuthController {

    @Autowired
    private final AuthenticationManager authenticationManager;

    @Autowired
    private final JwtUtil jwtUtil;

    @Autowired
    private final TokenCache tokenCache;
    
    @Autowired
    private UserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, TokenCache tokenCache) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.tokenCache = tokenCache;
    }

    @PostMapping("/token")
    public ResponseEntity<?> gerarToken(@RequestBody Credencial request) {
        try {
            // 1) Autentica credenciais
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            // 2) Carrega UserDetails
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

            // 3) Tenta reaproveitar token do cache
            String tokenExistente = tokenCache.getToken(request.username());
            if (tokenExistente != null && !jwtUtil.isTokenExpired(tokenExistente)) {
                long remainingSeconds = jwtUtil.getRemainingSeconds(tokenExistente);
                return ResponseEntity.ok(new AuthResponse(tokenExistente, remainingSeconds));
            }

            // 4) Gera novo token
            String novoToken = jwtUtil.generateToken(userDetails);
            tokenCache.storeToken(request.username(), novoToken);

            long defaultValiditySeconds = jwtUtil.getDefaultValiditySeconds();
            return ResponseEntity.ok(new AuthResponse(novoToken, defaultValiditySeconds));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciais inválidas"));
        }
    }

}



