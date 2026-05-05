package br.com.caqi.escolar.domain.repo;

import br.com.caqi.escolar.domain.entity.Escola;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EscolaRepository extends JpaRepository<Escola, Long> {
    Optional<Escola> findByInepId(String inepId);
}
