package br.com.caqi.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado no exchange caqi.events com routing key
 *   caqi.calculo.executado.{municipioId}
 * após cada cálculo CAQ/CAQi persistido com sucesso (1 por escola+etapa).
 *
 * Consumidores naturais: caq-compliance-svc (atualiza dashboards de gap),
 * caq-financeiro-svc (compara com execução orçamentária).
 */
@Schema(description = "Evento de domínio: cálculo CAQ/CAQi executado")
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
