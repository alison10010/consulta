package com.consulta.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_WINDOW_SECONDS = 5 * 60;

    // Tornada 'public' para ser visível a outras classes/pacotes
    public static class Counter { 
        // Os campos devem ser públicos ou ter getters
        public int attempts; 
        public long lastFailEpochSec;
    }

    // chave: username + "|" + ip
    private final Map<String, Counter> store = new ConcurrentHashMap<>();

    private String key(String username, String ip) {
        return (username == null ? "" : username.toLowerCase()) + "|" + (ip == null ? "" : ip);
    }

    public void loginFailed(String username, String ip) {
        String k = key(username, ip);
        Counter c = store.computeIfAbsent(k, kk -> new Counter());
        c.attempts++;
        c.lastFailEpochSec = Instant.now().getEpochSecond();
    }

    public void loginSucceeded(String username, String ip) {
        store.remove(key(username, ip));
    }

    public boolean isBlocked(String username, String ip) {
        Counter c = store.get(key(username, ip));
        if (c == null) return false;

        long now = Instant.now().getEpochSecond();
        long elapsed = now - c.lastFailEpochSec;

        if (elapsed > BLOCK_WINDOW_SECONDS) {
            store.remove(key(username, ip)); // janela expirou
            return false;
        }
        return c.attempts >= MAX_ATTEMPTS;
    }

    public long secondsToUnblock(String username, String ip) {
        Counter c = store.get(key(username, ip));
        if (c == null) return 0;
        long now = Instant.now().getEpochSecond();
        long elapsed = now - c.lastFailEpochSec;
        long left = BLOCK_WINDOW_SECONDS - elapsed;
        return Math.max(left, 0);
    }

    /**
     * Retorna o objeto Counter atual para fins de logging.
     */
    public Counter getAttemptCounters(String username, String ip) {
        String k = key(username, ip);
        Counter c = store.get(k);
        if (c == null) {
            Counter zero = new Counter();
            zero.attempts = 0;
            zero.lastFailEpochSec = 0;
            return zero;
        }
        return c;
    }
}