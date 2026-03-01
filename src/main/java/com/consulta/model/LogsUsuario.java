package com.consulta.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.consulta.Enum.AuthOutcome;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "logs_usuario")
public class LogsUsuario implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // quem tentou
    @Column(name = "username", length = 120)
    private String username;

    // de onde
    @Column(name = "ip", length = 64)
    private String ip;

    // resultado
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 32, nullable = false)
    private AuthOutcome outcome;

    // contadores/estado do antifraude
    @Column(name = "attempts") 
    private Integer attempts;

    @Column(name = "remaining") 
    private Integer remaining;

    // mensagens/erros
    @Column(name = "reason", length = 255)
    private String reason;
    
    @Column(name = "localizacao", length = 120)
    private String localizacao;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}