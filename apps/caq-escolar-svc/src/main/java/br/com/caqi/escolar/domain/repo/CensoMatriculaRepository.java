package br.com.caqi.escolar.domain.repo;

import br.com.caqi.escolar.domain.entity.CensoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CensoMatriculaRepository extends JpaRepository<CensoMatricula, Long> {

    List<CensoMatricula> findByImportacaoId(Long importacaoId);

    @Query("""
        SELECT m.etapaCodigo as etapaCodigo,
               m.tpCorRaca   as corRaca,
               COUNT(m)      as qtd
          FROM CensoMatricula m
         WHERE m.anoCenso = :anoCenso
         GROUP BY m.etapaCodigo, m.tpCorRaca
         ORDER BY m.etapaCodigo, m.tpCorRaca
    """)
    List<Object[]> agregarPorEtapaCorRaca(@Param("anoCenso") int anoCenso);
}
