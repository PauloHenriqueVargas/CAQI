package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    Optional<Insumo> findByCodigo(String codigo);
}
