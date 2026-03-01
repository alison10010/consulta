package com.consulta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.consulta.service.PagamentoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/mercadopago")
public class MercadoPagoWebhookController {

    private final PagamentoService pagamentoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MercadoPagoWebhookController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            
            // O Mercado Pago envia notificações de vários tipos. 
            // Para PIX, o que nos interessa é o "payment".
            String type = root.path("type").asText();
            
            if ("payment".equals(type)) {
                // O ID do pagamento vem dentro do nó "data"
                String mpPaymentId = root.path("data").path("id").asText();
                
                if (mpPaymentId != null && !mpPaymentId.isBlank()) {
                    // Chama o método que criamos no Service para validar e dar baixa
                    pagamentoService.verificarEAtualizarStatus(mpPaymentId);
                }
            }
            
            // Retornar 200 ou 201 é obrigatório para o Mercado Pago não reenviar a notificação
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            // Logamos o erro mas retornamos 200 para evitar loops de retry do MP se for erro de parse
            System.err.println("Erro ao processar Webhook: " + e.getMessage());
            return ResponseEntity.ok().build(); 
        }
    }
}