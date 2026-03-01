package com.consulta.security;

import java.util.Date;

public class AuthResponse {
    private String token;
    private long expiration; // timestamp
    
    public AuthResponse(String token, long expiration) {
        this.token = token;
        this.expiration = expiration;
    }

    public AuthResponse(String token, Date expiration) {
        this.token = token;
        this.expiration = expiration.getTime(); // milissegundos desde epoch
    }

    public String getToken() {
        return token;
    }

    public long getExpiration() {
        return expiration;
    }
}


