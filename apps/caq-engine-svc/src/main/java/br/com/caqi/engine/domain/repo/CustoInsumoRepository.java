package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.CustoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustoInsumoRepository extends JpaRepository<CustoInsumo, Long> {

    @Query("""
        SELECT c FROM CustoInsumo c
        WHERE c.insumoId = :insumoId
          AND c.perfil   = :perfil
          AND c.vigenciaInicio <= :data
          AND (c.vigenciaFim IS NULL OR c.vigenciaFim >= :data)
        ORDER BY c.vigenciaInicio DESC, c.id DESC
        """)
    List<CustoInsumo> findVigentes(@Param("insumoId") Long insumoId,
                                   @Param("perfil") String perfil,
                                   @Param("data") LocalDate data);

    default Optional<CustoInsumo> findVigente(Long insumoId, String perfil, LocalDate data) {
        return findVigentes(insumoId, perfil, data).stream().findFirst();
    }

    List<CustoInsumo> findByInsumoIdOrderByVigenciaInicioDesc(Long insumoId);
}
