package com.consulta.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.consulta.model.Usuario;
import com.consulta.record.AdminKpisDTO;
import com.consulta.record.EspecialidadeQtdDTO;

public interface RelatorioRepository extends JpaRepository<Usuario, Long> {

    @Query("""
        select new com.consulta.record.AdminKpisDTO(
            (select count(u) from Usuario u),
            (select count(u) from Usuario u where upper(u.acesso) = 'ADMIN'),
            (select count(u) from Usuario u where upper(u.acesso) = 'COLABORADOR'),
            (select count(u) from Usuario u where upper(u.acesso) = 'NORMAL'),

            (select count(h) from Horario h
              where h.data between :ini and :fim),

            (select count(h) from Horario h
              where h.data between :ini and :fim
                and h.disponivel = true
                and h.paciente is null),

            (select count(h) from Horario h
              where h.data between :ini and :fim
                and h.disponivel = false
                and h.paciente is not null),

            (select count(h) from Horario h
              where h.data between :ini and :fim
                and h.agendamentoPago = true),

            (select count(p) from Pagamento p
              where p.criadoEm between :iniDt and :fimDt),

            (select count(p) from Pagamento p
              where p.status = com.consulta.model.Pagamento.StatusPg.PAID
                and p.pagoEm is not null
                and p.pagoEm between :iniDt and :fimDt),

            (select coalesce(sum(p.valor), 0) from Pagamento p
              where p.status = com.consulta.model.Pagamento.StatusPg.PAID
                and p.pagoEm is not null
                and p.pagoEm between :iniDt and :fimDt)
        )
        from Usuario u
        where u.id = (select min(u2.id) from Usuario u2)
    """)
    AdminKpisDTO buscarKpis(@Param("ini") LocalDate ini,
                            @Param("fim") LocalDate fim,
                            @Param("iniDt") LocalDateTime iniDt,
                            @Param("fimDt") LocalDateTime fimDt);

    @Query("""
        select new com.consulta.record.EspecialidadeQtdDTO(e.especialidade, count(ue))
        from UsuarioEspecialidade ue
        join ue.especialidade e
        group by e.especialidade
        order by count(ue) desc
    """)
    List<EspecialidadeQtdDTO> rankingEspecialidades();
}