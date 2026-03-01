package com.consulta.record;

public record UsuarioDTO(
    String nome,
    String username,
    String password,
    String email,
    String cpf,
    String nascimento,
    String sexo,
    String telefone,
    
    // Campos de Endereço
    String cep,
    String endereco,
    String numero,
    String bairro,
    String cidade,
    String uf,
    
    // Campos Profissionais
    Long especialidade,
    String conselho,
    String registro,
    
    // Controle e Segurança
    String acesso, // 'NORMAL', 'COLABORADOR', 'ADMIN'
    boolean termoAceite
) {}
