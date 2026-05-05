package br.com.caqi.escolar.domain.repo;

import br.com.caqi.escolar.domain.entity.CensoMatriculaResumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CensoMatriculaResumoRepository extends JpaRepository<CensoMatriculaResumo, Long> {

    List<CensoMatriculaResumo> findByImportacaoId(Long importacaoId);

    List<CensoMatriculaResumo> findByAnoCensoOrderByEscolaInepAscEtapaCodigoAsc(Integer anoCenso);
}
