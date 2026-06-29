package com.consulta.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.consulta.Enum.StatusConsulta;
import com.consulta.model.Endereco;
import com.consulta.model.Horario;
import com.consulta.model.Pagamento;
import com.consulta.model.Usuario;
import com.consulta.repository.HorarioRepository;
import com.consulta.repository.PagamentoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import jakarta.annotation.PostConstruct;

@Service
public class PagamentoPagarMeService {

    public enum MetodoPagamento {
        PIX, CREDIT_CARD
    }

    private final HorarioRepository horarioRepository;
    private final PagamentoRepository pagamentoRepository;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${pagarme.secretKey}")
    private String pagarmeSecretKey;

    @Value("${pagarme.baseUrl:https://api.pagar.me/core/v5}")
    private String pagarmeBaseUrl;

    private String ordersUrl;
    private String chargesUrl;

    public PagamentoPagarMeService(
            HorarioRepository horarioRepository,
            PagamentoRepository pagamentoRepository
    ) {
        this.horarioRepository = horarioRepository;
        this.pagamentoRepository = pagamentoRepository;
    }

    @PostConstruct
    void init() {
        this.ordersUrl = pagarmeBaseUrl + "/orders";
        this.chargesUrl = pagarmeBaseUrl + "/charges";
    }

    @Transactional
    public Pagamento criarPagamentoPagarme(
            Long horarioId,
            BigDecimal valor,
            MetodoPagamento metodo,
            String cardToken,
            Integer installments
    ) {

        Horario h = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horário inválido"));

        int amount = valor
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .intValueExact();

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("items", List.of(Map.of(
                "amount", amount,
                "description", "Despachante " + h.getId(),
                "quantity", 1,
                "code", "HORARIO-" + h.getId()
        )));

        payload.put("customer", montarCustomer(h));

        payload.put("payments", List.of(
                montarPayment(metodo, cardToken, installments, h)
        ));

        HttpHeaders headers = pagarmeHeaders(
                idempotencyKeyFor(horarioId, metodo, amount)
        );

        ResponseEntity<String> resp = rest.postForEntity(
                ordersUrl,
                new HttpEntity<>(payload, headers),
                String.class
        );

        try {
            JsonNode root = om.readTree(resp.getBody());

            String orderId = root.path("id").asText(null);

            JsonNode charge = root.path("charges").path(0);
            String chargeId = charge.path("id").asText(null);
            String chargeStatus = charge.path("status").asText(null);

            JsonNode tx = charge.path("last_transaction");

            if (orderId == null || chargeId == null) {
                throw new RuntimeException("Resposta Pagar.me inválida: faltando orderId/chargeId.");
            }

            Pagamento pg = pagamentoRepository
                    .findByHorarioId(horarioId)
                    .orElse(new Pagamento());

            pg.setHorario(h);
            pg.setValor(valor);
            pg.setCriadoEm(LocalDateTime.now());

            pg.setPagarmeOrderId(orderId);
            pg.setPagarmeChargeId(chargeId);
            pg.setPagarmeChargeStatus(chargeStatus);
            pg.setPagarmePaymentMethod(metodo.name());

            pg.setPixCopiaECola(null);
            pg.setPixQrBase64(null);
            pg.setTicketUrl(null);

            if (metodo == MetodoPagamento.PIX) {
                String qrCode = tx.path("qr_code").asText(null);
                String qrCodeUrl = tx.path("qr_code_url").asText(null);

                pg.setPixCopiaECola(qrCode);
                pg.setTicketUrl(qrCodeUrl);

                if (qrCode != null && !qrCode.isBlank()) {
                    pg.setPixQrBase64(gerarQrBase64Png(qrCode, 240));
                }
            }

            aplicarStatusLocal(pg, chargeStatus);

            if (pg.getStatus() == Pagamento.StatusPg.PAID) {
                baixarHorarioPago(h);
                pg.setPagoEm(LocalDateTime.now());
            }

            return pagamentoRepository.save(pg);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Erro ao processar resposta do Pagar.me: " + e.getMessage(),
                    e
            );
        }
    }

    @Transactional
    public void verificarEAtualizarStatusPagarme(String pagarmeChargeId) {

        if (pagarmeChargeId == null || pagarmeChargeId.isBlank()) {
            return;
        }

        try {
            ResponseEntity<String> resp = rest.exchange(
                    chargesUrl + "/" + pagarmeChargeId,
                    HttpMethod.GET,
                    new HttpEntity<>(pagarmeHeaders(null)),
                    String.class
            );

            JsonNode root = om.readTree(resp.getBody());
            String status = root.path("status").asText("");

            Pagamento pg = pagamentoRepository.findByPagarmeChargeId(pagarmeChargeId)
                    .orElseThrow(() -> new RuntimeException("Pagamento não encontrado localmente"));

            pg.setPagarmeChargeStatus(status);

            aplicarStatusLocal(pg, status);

            if (pg.getStatus() == Pagamento.StatusPg.PAID) {
                pg.setPagoEm(LocalDateTime.now());
                baixarHorarioPago(pg.getHorario());
            }

            pagamentoRepository.save(pg);

        } catch (Exception e) {
            System.err.println("Erro ao verificar charge Pagar.me: " + e.getMessage());
        }
    }

    private Map<String, Object> montarCustomer(Horario h) {

        Usuario paciente = h.getPaciente();

        String nome = nvl(paciente != null ? paciente.getNome() : null, "JOAO DA SILVA COSTA");
        String email = nvl(paciente != null ? paciente.getEmail() : null, "alisonlimabandeira2@gmail.com");
        String cpf = onlyDigits(nvl(paciente != null ? paciente.getCpf() : null, "03646359242"));
        String id = paciente != null ? String.valueOf(paciente.getId()) : "1";

        Map<String, Object> customer = new LinkedHashMap<>();

        customer.put("name", nome);
        customer.put("email", email);
        customer.put("code", id);
        customer.put("document", cpf);
        customer.put("document_type", "CPF");
        customer.put("type", "individual");

        Map<String, String> mobile = new HashMap<>();
        mobile.put("country_code", "55");
        mobile.put("area_code", "68");
        mobile.put("number", "999998888");

        customer.put("phones", Map.of("mobile_phone", mobile));

        return customer;
    }

    private Map<String, Object> montarPayment(
            MetodoPagamento metodo,
            String cardToken,
            Integer installments,
            Horario h
    ) {

        if (metodo == MetodoPagamento.PIX) {
            return Map.of(
                    "payment_method", "pix",
                    "pix", Map.of("expires_in", 600)
            );
        }

        if (cardToken == null || cardToken.isBlank()) {
            throw new IllegalArgumentException("Token do cartão é obrigatório.");
        }

        int inst = installments == null ? 1 : Math.max(1, installments);

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("billing_address", montarBillingAddress(h));

        Map<String, Object> creditCard = new LinkedHashMap<>();
        creditCard.put("recurrence", false);
        creditCard.put("installments", inst);
        creditCard.put("statement_descriptor", "CONSULTA");
        creditCard.put("card_token", cardToken);
        creditCard.put("card", card);

        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("payment_method", "credit_card");
        payment.put("credit_card", creditCard);

        return payment;
    }

    private void aplicarStatusLocal(Pagamento pg, String statusPagarme) {

        if (statusPagarme == null || statusPagarme.isBlank()) {
            pg.setStatus(Pagamento.StatusPg.PENDING);
            return;
        }

        switch (statusPagarme.toLowerCase()) {
            case "paid":
                pg.setStatus(Pagamento.StatusPg.PAID);
                break;

            case "canceled":
            case "cancelled":
                pg.setStatus(Pagamento.StatusPg.CANCELED);
                break;

            case "failed":
            case "refused":
            case "not_authorized":
            case "payment_failed":
                pg.setStatus(Pagamento.StatusPg.REJECTED);
                break;

            default:
                pg.setStatus(Pagamento.StatusPg.PENDING);
                break;
        }
    }

    private void baixarHorarioPago(Horario h) {
        h.setAgendamentoPago(true);
        h.setStatus(StatusConsulta.MARCADA);
        horarioRepository.save(h);
    }

    private Map<String, Object> montarBillingAddress(Horario h) {

        Usuario paciente = h.getPaciente();
        Endereco e = paciente != null ? paciente.getEndereco() : null;

        String rua = "Rua de Teste";
        String numero = "123";
        String bairro = "Centro";
        String zip = "69900000";
        String city = "RIO BRANCO";
        String state = "AC";

        if (e != null) {
            rua = nvl(e.getEndereco(), rua);
            numero = nvl(e.getNumero(), numero);
            bairro = nvl(e.getBairro(), bairro);
            zip = onlyDigits(nvl(e.getCep(), zip));
            city = nvl(e.getCidade(), city);
            state = nvl(e.getEstado(), state);
        }

        if (state.length() > 2) {
            state = state.substring(0, 2);
        }

        String line1 = numero + "," + rua + "," + bairro;

        Map<String, Object> address = new LinkedHashMap<>();
        address.put("street", rua);
        address.put("number", numero);
        address.put("neighborhood", bairro);
        address.put("zip_code", zip);
        address.put("city", city);
        address.put("state", state);
        address.put("country", "BR");
        address.put("line_1", line1);

        return address;
    }

    private HttpHeaders pagarmeHeaders(String idempotencyKey) {

        String basic = Base64.getEncoder()
                .encodeToString((pagarmeSecretKey.trim() + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + basic);

        headers.set(
                "X-Idempotency-Key",
                idempotencyKey != null && !idempotencyKey.isBlank()
                        ? idempotencyKey
                        : UUID.randomUUID().toString()
        );

        return headers;
    }

    private String idempotencyKeyFor(Long horarioId, MetodoPagamento metodo, int amount) {

        String raw = "HORARIO:" + horarioId
                + "|MET:" + metodo.name()
                + "|AMT:" + amount;

        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String gerarQrBase64Png(String payload, int size) {

        try {
            var matrix = new QRCodeWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
            );

            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar QR base64: " + e.getMessage(), e);
        }
    }

    private String nvl(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }

    private String onlyDigits(String v) {
        return v == null ? "" : v.replaceAll("\\D", "");
    }
}