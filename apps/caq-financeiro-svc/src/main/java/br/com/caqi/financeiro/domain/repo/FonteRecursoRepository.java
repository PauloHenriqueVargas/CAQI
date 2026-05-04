package br.com.caqi.financeiro.domain.repo;

import br.com.caqi.financeiro.domain.entity.FonteRecurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FonteRecursoRepository extends JpaRepository<FonteRecurso, Long> {
    Optional<FonteRecurso> findByTipo(String tipo);
    List<FonteRecurso> findByTipoIn(List<String> tipos);
}
