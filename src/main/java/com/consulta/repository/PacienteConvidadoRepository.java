package com.consulta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.consulta.model.PacienteConvidado;

@Repository
public interface PacienteConvidadoRepository extends JpaRepository<PacienteConvidado, Long> {}
