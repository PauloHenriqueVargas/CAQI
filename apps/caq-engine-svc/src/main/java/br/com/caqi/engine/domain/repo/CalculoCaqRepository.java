package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.CalculoCaq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CalculoCaqRepository extends JpaRepository<CalculoCaq, Long> {
    Optional<CalculoCaq> findByEscolaIdAndEtapaIdAndAno(Long escolaId, Long etapaId, Integer ano);
}
