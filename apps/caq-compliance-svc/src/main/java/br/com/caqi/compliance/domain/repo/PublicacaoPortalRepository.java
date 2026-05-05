package br.com.caqi.compliance.domain.repo;

import br.com.caqi.compliance.domain.entity.PublicacaoPortal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicacaoPortalRepository extends JpaRepository<PublicacaoPortal, Long> {

    Optional<PublicacaoPortal> findFirstByTipoAndReferenciaOrderByDataPublicacaoDesc(
            String tipo, String referencia);

    List<PublicacaoPortal> findAllByOrderByDataPublicacaoDesc();

    List<PublicacaoPortal> findByTipoOrderByDataPublicacaoDesc(String tipo);

    List<PublicacaoPortal> findByReferenciaOrderByDataPublicacaoDesc(String referencia);
}
