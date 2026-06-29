package com.consulta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.consulta.model.Pagamento;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByHorarioId(Long horarioId);

    Optional<Pagamento> findByPagarmeOrderId(String pagarmeOrderId);

    Optional<Pagamento> findByPagarmeChargeId(String pagarmeChargeId);

}