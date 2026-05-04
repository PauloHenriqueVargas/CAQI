package br.com.caqi.compliance.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitário do método estático de hash do AuditChainService.
 * (Os métodos transacionais que tocam o repo serão cobertos por
 *  test de integração em Fase 3.C com Testcontainers.)
 */
class AuditChainServiceTest {

    @Test
    @DisplayName("computarHash devolve SHA-256 hex (64 chars) determinístico")
    void hashDeterministicoEHexa() {
        String h1 = AuditChainService.computarHash(
                AuditChainService.GENESIS_HASH,
                "notificacao", "1", "INSERT", "2026-05-03T22:00:00");
        String h2 = AuditChainService.computarHash(
                AuditChainService.GENESIS_HASH,
                "notificacao", "1", "INSERT", "2026-05-03T22:00:00");

        assertThat(h1).hasSize(64).matches("[0-9a-f]+").isEqualTo(h2);
    }

    @Test
    @DisplayName("Mudança em qualquer campo muda o hash (avalanche)")
    void mudancaEmQualquerCampoMudaHash() {
        String base = AuditChainService.computarHash(
                AuditChainService.GENESIS_HASH,
                "notificacao", "1", "INSERT", "2026-05-03T22:00:00");

        String diffTabela = AuditChainService.computarHash(
                AuditChainService.GENESIS_HASH,
                "calculo_caq", "1", "INSERT", "2026-05-03T22:00:00");
        String diffRegistroId = AuditChainService.computarHash(
                AuditChainService.GENESIS_HASH,
                "notificacao", "2", "INSERT", "2026-05-03T22:00:00");
        String diffAcao = AuditChainService.computarHash(
                AuditChainService.GENESIS_HASH,
                "notificacao", "1", "UPDATE", "2026-05-03T22:00:00");
        String diffTempo = AuditChainService.computarHash(
                AuditChainService.GENESIS_HASH,
                "notificacao", "1", "INSERT", "2026-05-03T22:00:01");
        String diffAnterior = AuditChainService.computarHash(
                "1".repeat(64),
                "notificacao", "1", "INSERT", "2026-05-03T22:00:00");

        assertThat(base).isNotEqualTo(diffTabela);
        assertThat(base).isNotEqualTo(diffRegistroId);
        assertThat(base).isNotEqualTo(diffAcao);
        assertThat(base).isNotEqualTo(diffTempo);
        assertThat(base).isNotEqualTo(diffAnterior);
    }

    @Test
    @DisplayName("GENESIS_HASH são 64 zeros (hex)")
    void genesisHashCorreto() {
        assertThat(AuditChainService.GENESIS_HASH).hasSize(64).isEqualTo("0".repeat(64));
    }
}
