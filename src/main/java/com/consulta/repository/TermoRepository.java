package com.consulta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.consulta.model.Termo;

@Repository
public interface TermoRepository extends JpaRepository<Termo, Long> {

	Termo findByUsuarioId(Long usuarioId);
	
}
