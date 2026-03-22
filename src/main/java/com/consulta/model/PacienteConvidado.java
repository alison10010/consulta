package com.consulta.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@Entity
public class PacienteConvidado implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "nome")
	private String nome;
	
	@Column(name = "username")
	private String username;
	
	@Column(name = "telefone")
	private String telefone;
	
	@Column(name = "celular")
	private String celular;
	
	@Column(name = "email")
	private String email;
	
	@Column(name = "sexo")
	private String sexo;
	
	@Column(name = "cpf")
	private String cpf;
	
	@Column(name = "nascimento")
	private String nascimento;	
	
	@Column(name = "possui_wpp")
	private boolean possuiWpp;

	@Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
	
	@Column(name = "data_update")
    @UpdateTimestamp
    private LocalDateTime dataUpdate;
	
	@PrePersist
    @PreUpdate
    private void ajustarMaiusculas() {
        if (nome != null) nome = nome.toUpperCase();
        if (email != null) email = email.toLowerCase();
    }

}
