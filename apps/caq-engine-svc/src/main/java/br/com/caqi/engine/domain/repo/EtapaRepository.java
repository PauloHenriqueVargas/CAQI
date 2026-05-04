package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.Etapa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtapaRepository extends JpaRepository<Etapa, Long> {
    Optional<Etapa> findByCodigo(String codigo);
}
