package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.CustoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface CustoInsumoRepository extends JpaRepository<CustoInsumo, Long> {

    @Query("""
        SELECT c FROM CustoInsumo c
        WHERE c.insumoId = :insumoId
          AND c.vigenciaInicio <= :data
          AND (c.vigenciaFim IS NULL OR c.vigenciaFim >= :data)
        ORDER BY c.vigenciaInicio DESC
        """)
    java.util.List<CustoInsumo> findVigentes(@Param("insumoId") Long insumoId, @Param("data") LocalDate data);

    default Optional<CustoInsumo> findVigente(Long insumoId, LocalDate data) {
        return findVigentes(insumoId, data).stream().findFirst();
    }
}
