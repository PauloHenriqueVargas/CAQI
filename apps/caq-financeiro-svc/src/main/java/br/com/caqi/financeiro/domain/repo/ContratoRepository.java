package br.com.caqi.financeiro.domain.repo;

import br.com.caqi.financeiro.domain.entity.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    List<Contrato> findByFornecedorIdOrderByDataAssinaturaDesc(Long fornecedorId);
}
