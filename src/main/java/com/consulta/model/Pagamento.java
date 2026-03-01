package com.consulta.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Pagamento implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum StatusPg {
        PENDING, PAID, REJECTED, CANCELED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_id", nullable = false, unique = true)
    private Horario horario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPg status = StatusPg.PENDING;

    // =========================================================
    // CAMPOS PARA MERCADO PAGO (PIX E CHECKOUT)
    // =========================================================

    // ID do Pagamento (Obrigatório para o seu novo Service e Webhook)
    @Column(length = 80)
    private String mpPaymentId;

    // ID da Preferência (Mantenha para evitar erro no Repository/Service antigo)
    @Column(length = 80)
    private String mpPreferenceId;

    // Link do checkout Pro (Opcional)
    @Column(length = 800)
    private String initPoint;

    // Dados específicos do PIX (Para exibir o QR Code no seu XHTML)
    @Column(columnDefinition = "TEXT")
    private String pixCopiaECola;

    @Column(columnDefinition = "TEXT")
    private String pixQrBase64;

    @Column(length = 800)
    private String ticketUrl;

    // Controle de Datas
    private LocalDateTime criadoEm = LocalDateTime.now();
    private LocalDateTime pagoEm;

    // Método auxiliar para pegar o ID de pagamento mesmo que esteja em campos diferentes
    public String getMpLastPaymentId() {
        return mpPaymentId;
    }
}