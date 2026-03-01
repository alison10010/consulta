package com.consulta.record;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiColaboradoresDTO {
	
	// ===== USUÁRIO =====
    private Long id;
    private String nome;
    private String username;
    private String email;
    private String especialidade;
    private String telefone;
    private String celular;
    private boolean possuiWpp;
    private String acesso;    

    // ===== ENDEREÇO =====
    private String cep;
    private String endereco;
    private String numero;
    private String bairro;
    private String cidade;
    private String estado;
    private String complemento;
    private String coordenadas;
    
    private List<Map<String, Object>> especialidades;
    
}
