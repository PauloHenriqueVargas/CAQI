package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    @Query("""
        SELECT COUNT(m) FROM Matricula m
        WHERE m.escolaId = :escolaId
          AND m.situacao = 'ativa'
        """)
    long contarAtivasPorEscola(@Param("escolaId") Long escolaId);

    @Query("""
        SELECT COUNT(m) FROM Matricula m
        WHERE m.escolaId = :escolaId
          AND m.etapaId  = :etapaId
          AND m.situacao = 'ativa'
        """)
    long contarAtivasPorEscolaEtapa(@Param("escolaId") Long escolaId, @Param("etapaId") Long etapaId);
}
