package com.consulta.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.consulta.Enum.StatusConsulta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "horario",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_horario_colab_data_hora",
        columnNames = {"colaborador_id", "data", "hora"}
    )
)
public class Horario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false)
    private boolean disponivel = true;

    @Column(name = "vaga")
    private String vaga;
    
    // Quem atende (COLABORADOR)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Usuario colaborador;

    // Quem marcou (NORMAL) - pode ser null quando estiver livre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Usuario paciente;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusConsulta status = StatusConsulta.PROCESSANDO;

    // controle do pagamento do agendamento
    @Column(nullable = false)
    private Boolean agendamentoPago = false;
    
    private LocalDateTime pixExpiraEm;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
	
	@Column(name = "data_update")
    @UpdateTimestamp
    private LocalDateTime dataUpdate;
}
