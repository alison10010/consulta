package com.consulta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.consulta.model.LogsUsuario;

@Repository
public interface LogsUsuarioRepository extends JpaRepository<LogsUsuario, Long> {}