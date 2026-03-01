package com.consulta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.consulta.model.UsuarioEspecialidade;

public interface UsuarioEspecialidadeRepository extends JpaRepository<UsuarioEspecialidade, Long> {

	  boolean existsByUsuarioIdAndEspecialidadeId(Long usuarioId, Long especialidadeId);

	  List<UsuarioEspecialidade> findByUsuarioId(Long usuarioId);
	  
	  List<UsuarioEspecialidade> findByUsuarioIdAndStatusNotIgnoreCase(Long usuarioId, String status);
	  
	  // ====== ADMIN: FILA DE ANÁLISE ======
	  List<UsuarioEspecialidade> findByStatusInOrderByCreatedAtDesc(List<String> status);
	  
	  @Query("""
        select ue from UsuarioEspecialidade ue
        join ue.usuario u
        join ue.especialidade e
        where upper(ue.status) in :status
          and (
             upper(u.nome) like upper(concat('%', :q, '%'))
             or upper(u.username) like upper(concat('%', :q, '%'))
             or upper(e.especialidade) like upper(concat('%', :q, '%'))
          )
        order by ue.createdAt desc
    """)
    List<UsuarioEspecialidade> buscarFila(@Param("status") List<String> status, @Param("q") String q);
}

