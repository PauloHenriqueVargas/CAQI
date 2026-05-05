package br.com.caqi.escolar.domain.repo;

import br.com.caqi.escolar.domain.entity.CensoImportacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CensoImportacaoRepository extends JpaRepository<CensoImportacao, Long> {

    Optional<CensoImportacao> findByAnoCensoAndArquivoHashSha256AndCodMunicipioIbge(
            Integer anoCenso, String hash, String codMunicipioIbge);

    List<CensoImportacao> findByAnoCensoOrderByCriadoEmDesc(Integer anoCenso);

    List<CensoImportacao> findAllByOrderByCriadoEmDesc();
}
