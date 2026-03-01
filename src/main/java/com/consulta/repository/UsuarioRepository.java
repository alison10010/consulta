package com.consulta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.consulta.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	
	Usuario findByUsername(String username); // VERIFICA NOME DE USER(EMAIL)
	
	@Query("""
	   select distinct u
	   from Usuario u
	   left join fetch u.endereco
	   where u.acesso = :acesso
	""")
	List<Usuario> findByAcessoComEndereco(@Param("acesso") String acesso);
	
	// retornar só as especialidades já usadas pelos usuários (COLABORADORES)
    @Query("""
       select distinct u.especialidade
	   from Usuario u
	   where u.acesso = 'COLABORADOR'
	     and u.especialidade is not null
	     and u.especialidade <> ''
	   order by u.especialidade
    """)
    List<String> listarEspecialidadesUsadas();

    Usuario findByHash(String hash);    
	
}
