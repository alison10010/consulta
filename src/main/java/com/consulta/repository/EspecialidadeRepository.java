package com.consulta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.consulta.model.Especialidade;

@Repository
public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {
	
	List<Especialidade> findByIdIn(List<Long> ids);
	
	boolean existsByEspecialidadeIgnoreCase(String especialidade);
	
	@Query("""
        select new com.consulta.record.EspecialidadeDTO(e.id, e.especialidade)
        from Especialidade e
        order by e.especialidade
    """)
    List<com.consulta.record.EspecialidadeDTO> listarDTO();

}
