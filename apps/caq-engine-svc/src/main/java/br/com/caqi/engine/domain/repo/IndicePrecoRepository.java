package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.IndicePreco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IndicePrecoRepository extends JpaRepository<IndicePreco, Long> {

    /**
     * Variações mensais de um índice em ordem cronológica entre duas competências (inclusive).
     * Ambas em formato YYYY-MM (string).
     */
    @Query("""
        SELECT i FROM IndicePreco i
        WHERE i.nome = :nome
          AND i.competencia >= :inicio
          AND i.competencia <= :fim
        ORDER BY i.competencia ASC
        """)
    List<IndicePreco> findVariacoes(@Param("nome") String nome,
                                    @Param("inicio") String competenciaInicio,
                                    @Param("fim") String competenciaFim);
}
