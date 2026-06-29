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
@Getter
@Setter
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
    @Column(nullable = false, length = 30)
    private StatusPg status = StatusPg.PENDING;

    // =========================================================
    // CAMPOS PAGAR.ME
    // =========================================================

    @Column(length = 100)
    private String pagarmeOrderId;

    @Column(length = 100)
    private String pagarmeChargeId;

    @Column(length = 60)
    private String pagarmeChargeStatus;

    @Column(length = 60)
    private String pagarmePaymentMethod;

    // =========================================================
    // PIX
    // =========================================================

    @Column(columnDefinition = "TEXT")
    private String pixCopiaECola;

    @Column(columnDefinition = "TEXT")
    private String pixQrBase64;

    @Column(length = 800)
    private String ticketUrl;

    private LocalDateTime criadoEm = LocalDateTime.now();

    private LocalDateTime pagoEm;

    public String getMpLastPaymentId() {
        return pagarmeChargeId;
    }
}