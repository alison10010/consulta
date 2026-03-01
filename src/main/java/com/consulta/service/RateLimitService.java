package com.consulta.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class RateLimitService {

    // 6 requisições permitidas por 60 segundos (1 minuto)
    private static final int MAX_REQUESTS = 6;
    private static final int TIME_WINDOW_SECONDS = 60;
    
    // O Cache: Chave = IP (String), Valor = Contador (AtomicInteger)
    private final Cache<String, AtomicInteger> requestCountsPerIp = Caffeine.newBuilder()
            // Configura a expiração: a entrada é removida 60 segundos após o último acesso/escrita
            .expireAfterWrite(TIME_WINDOW_SECONDS, TimeUnit.SECONDS)
            .maximumSize(100_000) 
            .build();

    /**
     * Verifica e registra a requisição para o IP fornecido .
     * @param ip O endereço IP do cliente.
     * @return true se o limite foi excedido, false caso contrário.
     */
    public boolean isRateLimitExceeded(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        // Obtém ou cria um novo contador (0) para o IP. Esta operação é thread-safe no Caffeine.
        AtomicInteger count = requestCountsPerIp.get(ip, key -> new AtomicInteger(0));
        
        // Aumenta o contador e obtém o valor *após* o incremento.
        int currentRequests = count.incrementAndGet(); 
        
        // Retorna true se o contador ultrapassou o limite.
        return currentRequests > MAX_REQUESTS;
    }
}