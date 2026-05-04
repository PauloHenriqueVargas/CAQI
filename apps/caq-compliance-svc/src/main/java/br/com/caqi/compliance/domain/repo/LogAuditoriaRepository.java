package br.com.caqi.compliance.domain.repo;

import br.com.caqi.compliance.domain.entity.LogAuditoria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    /**
     * Pega o último log na cadeia, com lock pessimista (PostgreSQL: FOR UPDATE)
     * para serializar inserções concorrentes — evita 2 logs com mesmo hash_antes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LogAuditoria l WHERE l.id = (SELECT MAX(l2.id) FROM LogAuditoria l2)")
    Optional<LogAuditoria> findUltimoComLock();

    List<LogAuditoria> findAllByOrderByIdAsc();
}
