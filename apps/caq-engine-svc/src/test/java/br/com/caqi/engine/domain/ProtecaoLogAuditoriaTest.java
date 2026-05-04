package br.com.caqi.engine.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica que a migration V0006 bloqueia UPDATE/DELETE/TRUNCATE em
 * log_auditoria via REVOKE + trigger BEFORE — defesa em profundidade
 * da cadeia SHA-256 (Merkle).
 *
 * INSERT continua funcionando (pré-condição para o AuditChainService).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("V0006 — log_auditoria é append-only (UPDATE/DELETE/TRUNCATE bloqueados)")
class ProtecaoLogAuditoriaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("caqi_test")
            .withUsername("caqi")
            .withPassword("caqi_test_password");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void insertPermitido_updateDeleteTruncateBloqueados() {
        // INSERT — permitido (cadeia precisa crescer)
        jdbc.update(
                "INSERT INTO log_auditoria (tabela, registro_id, acao, hash_antes, hash_depois) " +
                        "VALUES (?, ?, ?, ?, ?)",
                "test_tabela", "1", "INSERT",
                "0".repeat(64),
                "a".repeat(64)
        );
        Long id = jdbc.queryForObject("SELECT MAX(log_id) FROM log_auditoria", Long.class);
        assertThat(id).isNotNull().isPositive();

        // UPDATE — bloqueado pelo trigger
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE log_auditoria SET acao = 'TAMPERED' WHERE log_id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");

        // DELETE — bloqueado
        assertThatThrownBy(() -> jdbc.update(
                "DELETE FROM log_auditoria WHERE log_id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");

        // TRUNCATE — bloqueado (statement-level trigger)
        assertThatThrownBy(() -> jdbc.execute("TRUNCATE TABLE log_auditoria"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");

        // Confirmação: o registro inserido continua intacto
        String acao = jdbc.queryForObject(
                "SELECT acao FROM log_auditoria WHERE log_id = ?", String.class, id);
        assertThat(acao).isEqualTo("INSERT");
    }

    @Test
    @DisplayName("Procedimento de exceção: DISABLE TRIGGER permite, mas exige superuser")
    void disableTriggerExigeSuperuser() {
        // O usuário 'caqi' do Testcontainer é o owner do DB, então ele PODE
        // desabilitar o trigger (mesmo sem ser superuser do cluster).
        // Em prod, o usuário aplicacional não deve ter privilégio de owner —
        // só superuser/DBA consegue. Este teste documenta a saída de
        // emergência referenciada no runbook.

        jdbc.execute("ALTER TABLE log_auditoria DISABLE TRIGGER prevent_log_auditoria_changes");
        // INSERT pré-condição
        jdbc.update(
                "INSERT INTO log_auditoria (tabela, registro_id, acao, hash_antes, hash_depois) " +
                        "VALUES ('exc', '99', 'INSERT', ?, ?)",
                "0".repeat(64), "b".repeat(64)
        );
        Long id = jdbc.queryForObject("SELECT MAX(log_id) FROM log_auditoria", Long.class);
        // Com trigger DISABLED, UPDATE não é bloqueado
        int updated = jdbc.update(
                "UPDATE log_auditoria SET acao = 'EMERGENCIA_DOCUMENTADA' WHERE log_id = ?", id);
        assertThat(updated).isEqualTo(1);

        // Religa o trigger imediatamente
        jdbc.execute("ALTER TABLE log_auditoria ENABLE TRIGGER prevent_log_auditoria_changes");
        // E volta a bloquear
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE log_auditoria SET acao = 'X' WHERE log_id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }
}
