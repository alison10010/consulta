package com.consulta.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "usuario_especialidade")
public class UsuarioEspecialidade implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  @ManyToOne(optional = false)
  @JoinColumn(name = "especialidade_id")
  private Especialidade especialidade;

  @Column(name = "valor")
  private BigDecimal valor;
  
  @Column(name = "valor_com_guia")
  private BigDecimal valorComGuia;
  
  @Column(name = "conselho")
  private String conselho;
  
  @Column(name = "registro")
  private String registro;
  
  @Column(name = "path_diploma_especialidade")
  private String pathDiplomaEspecialidade;
	
  @Column(name = "path_carteira_especialidade")
  private String pathCarteiraEspecialidade;

  @Column(name = "nome_arquivo")
  private String nomeArquivo;
  
  @Column(name = "status")
  private String status;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "data_update")
  private LocalDateTime dataUpdate;
}

