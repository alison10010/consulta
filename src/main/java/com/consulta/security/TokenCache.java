package com.consulta.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class TokenCache {
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    public String getToken(String username) {
        return tokens.get(username);
    }

    public void storeToken(String username, String token) {
        tokens.put(username, token);
    }

    public void removeToken(String username) {
        tokens.remove(username);
    }
}

