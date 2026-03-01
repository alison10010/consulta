package com.consulta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.consulta.Enum.StatusConsulta;
import com.consulta.model.Horario;
import com.consulta.model.Pagamento;
import com.consulta.repository.HorarioRepository;
import com.consulta.repository.PagamentoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PagamentoService {

    private final HorarioRepository horarioRepository;
    private final PagamentoRepository pagamentoRepository;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${mercadopago.accessToken}")
    private String accessToken;

    @Value("${app.baseUrl}")
    private String baseUrl;

    private static final String MP_API = "https://api.mercadopago.com/v1/payments";

    public PagamentoService(HorarioRepository horarioRepository, PagamentoRepository pagamentoRepository) {
        this.horarioRepository = horarioRepository;
        this.pagamentoRepository = pagamentoRepository;
    }

    @Transactional
    public Pagamento gerarPix(Long horarioId, BigDecimal valor) {
        Horario h = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horário inválido"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transaction_amount", valor.setScale(2, RoundingMode.HALF_UP));
        payload.put("payment_method_id", "pix");
        payload.put("external_reference", "HORARIO-" + horarioId);
        payload.put("notification_url", baseUrl + "/consulta/api/mercadopago/webhook");
        
        String email = (h.getPaciente() != null) ? h.getPaciente().getEmail() : "test_user@test.com";
        payload.put("payer", Map.of("email", email));

        payload.put("date_of_expiration", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.trim());
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

        try {
            ResponseEntity<String> resp = rest.postForEntity(MP_API, new HttpEntity<>(payload, headers), String.class);
            
            // --- DEBUG: Imprima a resposta para ver o que o Mercado Pago enviou ---
            System.out.println("RESPOSTA MP: " + resp.getBody());

            JsonNode root = om.readTree(resp.getBody());
            
            // Acessando o ID de forma mais segura
            JsonNode idNode = root.get("id");
            String paymentId = (idNode != null) ? idNode.asText() : null;

            if (paymentId == null || paymentId.isEmpty()) {
                throw new RuntimeException("ID do pagamento não foi retornado pelo Mercado Pago");
            }

            JsonNode txData = root.path("point_of_interaction").path("transaction_data");

            Pagamento pg = pagamentoRepository.findByHorarioId(horarioId).orElse(new Pagamento());
            pg.setHorario(h);
            
            // Atribuindo os valores
            pg.setMpPaymentId(paymentId); 
            pg.setPixCopiaECola(txData.path("qr_code").asText());
            pg.setPixQrBase64(txData.path("qr_code_base64").asText());
            pg.setTicketUrl(txData.path("ticket_url").asText());
            pg.setStatus(Pagamento.StatusPg.PENDING);
            pg.setCriadoEm(LocalDateTime.now());
            pg.setValor(valor); 

            System.out.println("Salvando pagamento com ID: " + paymentId);

            return pagamentoRepository.save(pg);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao gerar PIX: " + e.getMessage());
        }
    }

    @Transactional
    public void verificarEAtualizarStatus(String mpPaymentId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken.trim());
            
            ResponseEntity<String> resp = rest.exchange(MP_API + "/" + mpPaymentId, 
                                            HttpMethod.GET, new HttpEntity<>(headers), String.class);
            
            JsonNode root = om.readTree(resp.getBody());
            String statusMP = root.path("status").asText();
            
            Pagamento pg = pagamentoRepository.findByMpPaymentId(mpPaymentId)
                    .orElseThrow(() -> new RuntimeException("Pagamento não encontrado localmente"));

            if ("approved".equalsIgnoreCase(statusMP)) {
                pg.setStatus(Pagamento.StatusPg.PAID);
                pg.setPagoEm(LocalDateTime.now());
                
                Horario h = pg.getHorario();
                h.setAgendamentoPago(true);
                h.setStatus(StatusConsulta.MARCADA);
                horarioRepository.save(h);
            }
            pagamentoRepository.save(pg);
        } catch (Exception e) {
            System.err.println("Erro na verificação: " + e.getMessage());
        }
    }
}