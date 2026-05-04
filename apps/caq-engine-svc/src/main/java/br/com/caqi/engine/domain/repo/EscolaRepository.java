package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.Escola;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EscolaRepository extends JpaRepository<Escola, Long> {
    Optional<Escola> findByInepId(String inepId);
    Optional<Escola> findByNome(String nome);
}
