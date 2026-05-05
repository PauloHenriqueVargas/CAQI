package br.com.caqi.escolar.domain.repo;

import br.com.caqi.escolar.domain.entity.Etapa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtapaRepository extends JpaRepository<Etapa, Long> {
    Optional<Etapa> findByCodigo(String codigo);
}
