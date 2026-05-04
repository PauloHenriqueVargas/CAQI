package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.ParametroEtapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ParametroEtapaRepository extends JpaRepository<ParametroEtapa, Long> {

    /**
     * Devolve o parâmetro vigente na data informada para a etapa.
     * Se houver mais de um, pega o de maior vigencia_inicio (mais recente que ainda esteja vigente).
     */
    @Query("""
        SELECT p FROM ParametroEtapa p
        WHERE p.etapaId = :etapaId
          AND p.vigenciaInicio <= :data
          AND (p.vigenciaFim IS NULL OR p.vigenciaFim >= :data)
        ORDER BY p.vigenciaInicio DESC
        """)
    java.util.List<ParametroEtapa> findVigentes(@Param("etapaId") Long etapaId, @Param("data") LocalDate data);

    default Optional<ParametroEtapa> findVigente(Long etapaId, LocalDate data) {
        return findVigentes(etapaId, data).stream().findFirst();
    }
}
