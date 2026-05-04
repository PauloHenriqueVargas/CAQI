package br.com.caqi.financeiro.domain.repo;

import br.com.caqi.financeiro.domain.entity.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {

    @Query("""
        SELECT COALESCE(SUM(r.valor), 0) FROM Receita r
        WHERE r.competencia BETWEEN :inicio AND :fim
          AND r.origem IN :origens
        """)
    BigDecimal somarPorOrigemNoAno(@Param("inicio") String competenciaInicio,
                                   @Param("fim") String competenciaFim,
                                   @Param("origens") List<String> origens);

    @Query("""
        SELECT r FROM Receita r
        WHERE r.competencia BETWEEN :inicio AND :fim
        ORDER BY r.competencia, r.id
        """)
    List<Receita> listarDoAno(@Param("inicio") String competenciaInicio,
                              @Param("fim") String competenciaFim);
}
