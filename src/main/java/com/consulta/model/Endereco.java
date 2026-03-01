package com.consulta.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
public class Endereco implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String cep;
	
	private String endereco;
	
	private String numero;
	
	private String bairro;
	
	private String cidade;
	
	private String estado;
		
	private String complemento;
	
	private String coordenadas;
	
	@OneToOne
	@JoinColumn(name = "id_usuario", referencedColumnName = "id")
	private Usuario usuario;
	
	@PrePersist
    @PreUpdate
    private void ajustarMaiusculas() {
        if (endereco != null) endereco = endereco.toUpperCase();
        if (bairro != null) bairro = bairro.toUpperCase();
        if (cidade != null) cidade = cidade.toUpperCase();
        if (estado != null) estado = estado.toUpperCase();
        if (estado != null) estado = estado.toUpperCase();
    }
	
}
