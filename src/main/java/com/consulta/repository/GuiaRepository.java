package com.consulta.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.consulta.model.Guia;
import com.consulta.model.Usuario;

public interface GuiaRepository extends JpaRepository<Guia, Long> {

    Optional<Guia> findByCodigo(String codigo);

    List<Guia> findByPacienteOrderByEmitidaEmDesc(Usuario paciente);

    Optional<Guia> findByHorarioId(Long horarioId);

    boolean existsByCodigo(String codigo);
    
    // ===== FETCH por ID =====
    @Query("""
        select g
          from Guia g
          join fetch g.paciente p
          join fetch g.colaborador c
          join fetch g.horario h
         where g.id = :id
    """)
    Optional<Guia> buscarGuiaFetch(@Param("id") Long id);
    
    // ===== FETCH por CÓDIGO =====
    @Query("""
        select g
          from Guia g
          join fetch g.paciente p
          join fetch g.colaborador c
          join fetch g.horario h
         where g.codigo = :codigo
    """)
    Optional<Guia> buscarGuiaPorCodigoFetch(@Param("codigo") String codigo);
    
    @Query("""
	   select g
	     from Guia g
	     join fetch g.paciente p
	     join fetch g.colaborador c
	     join fetch g.horario h
	    where g.colaborador = :colab
	      and g.emitidaEm between :ini and :fim
	      and (:status is null or g.status = :status)
	    order by g.emitidaEm desc
	""")
	List<Guia> listarRelatorioColaborador(@Param("colab") Usuario colab,
	                                     @Param("ini") LocalDateTime ini,
	                                     @Param("fim") LocalDateTime fim,
	                                     @Param("status") Guia.StatusGuia status);
}


