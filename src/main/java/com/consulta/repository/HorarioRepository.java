package com.consulta.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.consulta.model.Horario;
import com.consulta.model.Usuario;

import jakarta.transaction.Transactional;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    boolean existsByColaboradorAndDataAndHora(Usuario colaborador, LocalDate data, LocalTime hora);

    @Query("""
    	    SELECT h
    	    FROM Horario h
    	    WHERE h.colaborador = :colaborador
    	      AND h.data = :data
    	      AND h.disponivel = true
    	      AND (
    	            :data <> CURRENT_DATE
    	            OR h.hora >= :horaAtual
    	          )
    	    ORDER BY h.hora ASC
    """)
    List<Horario> buscarHorariosValidos(
    		@Param("colaborador") Usuario colaborador,
            @Param("data") LocalDate data,
            @Param("horaAtual") LocalTime horaAtual);
    
    
    // ✅ remove horários livres e vencidos: data < hoje OR (data = hoje AND hora < agora)
    @Modifying
    @Transactional
    @Query("""
        delete from Horario h
         where h.disponivel = true
           and h.paciente is null
           and (
                h.data < :hoje
                or (h.data = :hoje and h.hora < :agora)
           )
    """)
    int deleteHorariosVaziosVencidos(@Param("hoje") LocalDate hoje,
                                    @Param("agora") LocalTime agora);
    
    // consultas marcadas do paciente (ordenadas)
    @Query("""
	   select h from Horario h
	   join fetch h.colaborador c
	   left join fetch c.endereco
	   where h.paciente = :paciente
	   order by h.data asc, h.hora asc
	""")
	List<Horario> listarConsultasPacienteComColaborador(@Param("paciente") Usuario paciente);


    // (opcional) para cancelar com segurança: garante que o horário é do paciente logado
    java.util.Optional<Horario> findByIdAndPaciente(Long id, Usuario paciente);
    
 // ====== AGENDA DO COLABORADOR (dia) ======
    @Query("""
       select h
       from Horario h
       left join fetch h.paciente p
       where h.colaborador = :colaborador
         and h.data = :data
       order by h.dataUpdate desc
    """)
    List<Horario> listarAgendaDiaComPaciente(@Param("colaborador") Usuario colaborador,
                                            @Param("data") LocalDate data);

    // para validar que o horário pertence ao colaborador logado
    java.util.Optional<Horario> findByIdAndColaborador(Long id, Usuario colaborador);

    // remove somente se estiver LIVRE (disponivel=true e paciente null)
    @Modifying
    @Transactional
    @Query("""
       delete from Horario h
       where h.id = :id
         and h.colaborador = :colaborador
         and h.disponivel = true
         and h.paciente is null
    """)
    int deletarHorarioLivreDoColaborador(@Param("id") Long id,
                                         @Param("colaborador") Usuario colaborador);
    
    // LIMIT DE BAGAS POR DIA
    long countByColaboradorAndData(Usuario colaborador, LocalDate data);
    
    
 // ====== PARA GERAR GUIA: traz colaborador + endereco + paciente carregados ======
    @Query("""
        select h
          from Horario h
          join fetch h.colaborador c
          left join fetch c.endereco e
          left join fetch h.paciente p
         where h.id = :id
    """)
    Optional<Horario> buscarHorarioParaGuia(@Param("id") Long id);

    
    @Query("""
	   select h
	   from Horario h
	   where h.colaborador.id = :colabId
	     and h.disponivel = true
	     and h.paciente is null
	     and h.data between :ini and :fim
	   order by h.data asc, h.hora asc
	""")
	List<Horario> listarDisponiveisPeriodo(@Param("colabId") Long colabId,
	                                      @Param("ini") LocalDate ini,
	                                      @Param("fim") LocalDate fim);

    @Query("""
	   select h
	     from Horario h
	     left join fetch h.paciente p
	    where h.colaborador = :colaborador
	      and h.data between :ini and :fim
	    order by h.data asc, h.hora asc
	""")
	List<Horario> listarRelatorioColaborador(@Param("colaborador") Usuario colaborador,
	                                        @Param("ini") LocalDate ini,
	                                        @Param("fim") LocalDate fim);


    @Modifying
    @Query("""
        update Horario h
           set h.paciente = null,
               h.disponivel = true,
               h.agendamentoPago = false,
               h.status = com.consulta.Enum.StatusConsulta.PROCESSANDO,
               h.pixExpiraEm = null
         where h.status = com.consulta.Enum.StatusConsulta.PROCESSANDO
           and h.agendamentoPago = false
           and h.disponivel = false
           and h.pixExpiraEm is not null
           and h.pixExpiraEm <= :agora
    """)
    int liberarHorariosExpirados(@Param("agora") LocalDateTime agora);

    
}
