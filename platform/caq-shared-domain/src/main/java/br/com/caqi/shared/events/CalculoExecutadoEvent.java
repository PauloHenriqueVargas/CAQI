package br.com.caqi.shared.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado por caq-engine-svc no exchange caqi.events com routing key
 *   caqi.calculo.executado.{tenantMunicipioId}
 * após cada cálculo CAQ/CAQi persistido (1 por escola+etapa).
 *
 * Consumidores: caq-compliance-svc (auditoria + avaliação), caq-financeiro-svc
 * (correlação com execução), web BFF (notificação UI).
 *
 * Esta é a contract canônica — produtor e consumidor MUST usar este FQN.
 */
public record CalculoExecutadoEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantMunicipioId,
        Long calculoId,
        Long escolaId,
        String etapaCodigo,
        Integer ano,
        BigDecimal valorCaqi,
        BigDecimal valorCaq,
        BigDecimal gap
) {
    public static CalculoExecutadoEvent novo(
            String municipioId, Long calculoId, Long escolaId, String etapaCodigo, Integer ano,
            BigDecimal caqi, BigDecimal caq, BigDecimal gap
    ) {
        return new CalculoExecutadoEvent(
                UUID.randomUUID(), Instant.now(), municipioId,
                calculoId, escolaId, etapaCodigo, ano, caqi, caq, gap
        );
    }
}
