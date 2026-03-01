package com.consulta.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "guia", indexes = {
        @Index(name = "idx_guia_codigo", columnList = "codigo", unique = true),
        @Index(name = "idx_guia_paciente", columnList = "paciente_id"),
        @Index(name = "idx_guia_colaborador", columnList = "colaborador_id")
})
public class Guia implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum StatusGuia {
        GERADA, USADA, CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código único da guia (ex: 8-12 chars)
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;
 
    @Column(name = "especialidade")
    private String especialidade;

    @Column(nullable = false)
    private LocalDateTime emitidaEm;

    // Ate o dia da consulta
    private LocalDate validade;

    // ====== Desconto ======
    @Column(precision = 10, scale = 2)
    private BigDecimal percentualDesconto; // ex 10.00

    @Column(precision = 12, scale = 2)
    private BigDecimal valorOriginal;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorComDesconto;

    // ====== Referências ======
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Usuario colaborador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id", nullable = false)
    private Horario horario;

    // ====== “snapshot” do endereço (pra não quebrar se o colaborador mudar o endereço depois) ======
    @Column(length = 120)
    private String endereco;

    @Column(length = 30)
    private String numero;

    @Column(length = 80)
    private String bairro;

    @Column(length = 80)
    private String cidade;

    @Column(length = 30)
    private String estado;

    @Column(length = 15)
    private String cep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusGuia status;

    // URL/Hash para validar (opcional)
    @Column(length = 120)
    private String urlValidacao;
}
