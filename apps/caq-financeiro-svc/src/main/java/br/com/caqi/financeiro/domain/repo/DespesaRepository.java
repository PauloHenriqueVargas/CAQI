package br.com.caqi.financeiro.domain.repo;

import br.com.caqi.financeiro.domain.entity.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    /** Soma despesas com siope_grupo informado no ano. */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0) FROM Despesa d
        WHERE d.competencia BETWEEN :inicio AND :fim
          AND d.siopeGrupo = :siopeGrupo
        """)
    BigDecimal somarPorSiopeGrupoNoAno(@Param("inicio") String inicio,
                                       @Param("fim") String fim,
                                       @Param("siopeGrupo") String siopeGrupo);

    /** Soma despesas de pessoal (natureza começa com '3.1') de fontes informadas no ano. */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0) FROM Despesa d
        WHERE d.competencia BETWEEN :inicio AND :fim
          AND d.natureza LIKE '3.1%'
          AND d.fonteRecursoId IN (
              SELECT f.id FROM br.com.caqi.financeiro.domain.entity.FonteRecurso f WHERE f.tipo IN :fontesTipo
          )
        """)
    BigDecimal somarPessoalPorFonteNoAno(@Param("inicio") String inicio,
                                         @Param("fim") String fim,
                                         @Param("fontesTipo") List<String> fontesTipo);

    /** Soma despesas de capital (natureza começa com '4') de fontes informadas no ano. */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0) FROM Despesa d
        WHERE d.competencia BETWEEN :inicio AND :fim
          AND d.natureza LIKE '4%'
          AND d.fonteRecursoId IN (
              SELECT f.id FROM br.com.caqi.financeiro.domain.entity.FonteRecurso f WHERE f.tipo IN :fontesTipo
          )
        """)
    BigDecimal somarCapitalPorFonteNoAno(@Param("inicio") String inicio,
                                         @Param("fim") String fim,
                                         @Param("fontesTipo") List<String> fontesTipo);

    @Query("""
        SELECT d FROM Despesa d
        WHERE d.competencia BETWEEN :inicio AND :fim
        ORDER BY d.competencia, d.id
        """)
    List<Despesa> listarDoAno(@Param("inicio") String inicio, @Param("fim") String fim);
}
