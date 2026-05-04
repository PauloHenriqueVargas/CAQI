package br.com.caqi.compliance.domain.repo;

import br.com.caqi.compliance.domain.entity.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByStatusOrderByCreatedAtDesc(String status);
    List<Notificacao> findByAnoReferenciaOrderByCreatedAtDesc(Integer anoReferencia);
    List<Notificacao> findByTipoAndAnoReferencia(String tipo, Integer anoReferencia);
}
