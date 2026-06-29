package com.consulta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.consulta.service.PagamentoPagarMeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/pagarme")
public class PagarmeWebhookController {

    private final PagamentoPagarMeService pagamentoService;
    private final ObjectMapper om = new ObjectMapper();

    public PagarmeWebhookController(PagamentoPagarMeService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String body) {
        try {
            JsonNode root = om.readTree(body);
            String type = root.path("type").asText("");

            // Caso seja evento de CHARGE, normalmente data.id é o charge_id (ch_xxx)
            if (type.startsWith("charge.")) {
                String chargeId = root.path("data").path("id").asText(null);
                if (chargeId != null && !chargeId.isBlank()) {
                    pagamentoService.verificarEAtualizarStatusPagarme(chargeId);
                }
            }

            // Caso seja evento de ORDER (order.paid), data.id é order_id e tem charges[] dentro :contentReference[oaicite:13]{index=13}
            if (type.startsWith("order.")) {
                JsonNode charges = root.path("data").path("charges");
                if (charges.isArray() && charges.size() > 0) {
                    String chargeId = charges.path(0).path("id").asText(null);
                    if (chargeId != null && !chargeId.isBlank()) {
                        pagamentoService.verificarEAtualizarStatusPagarme(chargeId);
                    }
                }
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("Erro webhook Pagar.me: " + e.getMessage());
            // Retorna 200 pra evitar loop de retry por erro de parse, como você já fazia
            return ResponseEntity.ok().build();
        }
    }
}
