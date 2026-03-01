package com.consulta.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "MySecret-s3gur@-acreprevidencia2005-Key#!";
    private static final long DEFAULT_VALIDITY_SECONDS = 3600L; // 1 hora em segundos

    // --------- Chave ---------
    private SecretKey getSecretKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    // --------- Helpers internos ---------
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSecretKey())
                .parseClaimsJws(token)
                .getBody();
    }

    private String buildToken(Map<String, Object> claims, String subject, long validitySeconds) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validitySeconds * 1000L);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSecretKey())
                .compact();
    }

    // --------- Geração ---------
    /** Gera um token com validade padrão (3600s) e roles do usuário. */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return buildToken(claims, userDetails.getUsername(), DEFAULT_VALIDITY_SECONDS);
    }

    /** Gera token com validade customizada em segundos. */
    public String generateToken(UserDetails userDetails, long validitySeconds) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return buildToken(claims, userDetails.getUsername(), validitySeconds);
    }

    // --------- Extrações ---------
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Date getExpirationDate(String token) {
        try {
            return parseClaims(token).getExpiration();
        } catch (JwtException e) {
            return null;
        }
    }

    /** Expiração em epoch millis (0 se não conseguir ler). */
    public long getExpirationEpochMillis(String token) {
        Date exp = getExpirationDate(token);
        return (exp == null) ? 0L : exp.getTime();
    }

    /** Segundos restantes até expirar (0 se inválido/expirado). */
    public long getRemainingSeconds(String token) {
        try {
            long diffMs = parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
            return (diffMs > 0) ? diffMs / 1000L : 0L;
        } catch (JwtException e) {
            return 0L;
        }
    }

    // --------- Validação ---------
    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (JwtException e) {
            return true; // inválido ou expirado
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            String username = claims.getSubject();
            return username != null
                    && username.equals(userDetails.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // --------- Refresh ---------
    /** 
     * Renova o token (mesmo subject e roles) se ainda for válido.
     * Lança ExpiredJwtException se já estiver expirado.
     */
    public String refreshIfValid(String token) {
        Claims c = parseClaims(token); // lança JwtException se inválido
        if (c.getExpiration().before(new Date())) {
            throw new ExpiredJwtException(null, c, "Token expirado");
        }
        Map<String, Object> claims = new HashMap<>();
        Object roles = c.get("roles");
        if (roles != null) claims.put("roles", roles);
        return buildToken(claims, c.getSubject(), DEFAULT_VALIDITY_SECONDS);
    }

    // --------- Utilidades ---------
    public long getDefaultValiditySeconds() {
        return DEFAULT_VALIDITY_SECONDS;
    }
}
