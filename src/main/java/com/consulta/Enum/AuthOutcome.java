package com.consulta.Enum;

public enum AuthOutcome {
    SUCCESS,       // Login bem-sucedido
    FAILURE,       // Falha por credencial inválida (BadCredentials)
    LOCKED,        // Bloqueado por política de brute force
    ACCESS_DENIED, // Não possui as roles/autoridades necessárias (InsufficientAuthentication)
    INTERNAL_ERROR // Erro interno do sistema/API (AuthenticationService)
}