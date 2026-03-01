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
public class Usuario implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "nome")
	private String nome;
	
	@Column(name = "username")
	private String username;

	@Column(name = "password")
	private String password;
	
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
	
	@Column(name = "acesso")
	private String acesso; /* NIVEL DE ACESSO ('NORMAL','COLABORADOR','ADMIN') */
	
	@Column(name = "emai_confirmado")
	private boolean emaiConfirmado;
	
	@Column(name = "termo_aceite")
	private boolean termoAceite;
	
	@Column(name = "especialidade")
	private String especialidade;
	
	@ManyToMany
	@JoinTable(
	    name = "usuario_especialidade",
	    joinColumns = @JoinColumn(name = "usuario_id"),
	    inverseJoinColumns = @JoinColumn(name = "especialidade_id")
	)
	private Set<Especialidade> especialidades = new HashSet<>();
	
	@Column(name = "conselho")
    private String conselho; // CRM, CRP, etc.
	
	@Column(name = "registro_profissional")
    private String registro;
	
	@OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_endereco", referencedColumnName = "id")
    private Endereco endereco;  
	
	@Column(name = "hash")
	private String hash;	
	
	@Column(name = "path_diploma")
	private String pathDiploma;

	@Column(name = "path_carteira")
	private String pathCarteira;
	
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
        if (acesso != null) acesso = acesso.toUpperCase();
        if (email != null) email = email.toLowerCase();
    }

}
