package br.com.caqi.financeiro.domain.repo;

import br.com.caqi.financeiro.domain.entity.MedicaoContrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicaoContratoRepository extends JpaRepository<MedicaoContrato, Long> {
    List<MedicaoContrato> findByContratoIdOrderByCompetenciaDesc(Long contratoId);
}
