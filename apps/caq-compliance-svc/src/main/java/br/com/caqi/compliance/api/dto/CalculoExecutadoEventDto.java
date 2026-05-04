package br.com.caqi.compliance.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mirror do CalculoExecutadoEvent publicado por caq-engine-svc.
 * Os field names DEVEM bater (Jackson deserialization). Qualquer mudança
 * no produtor exige coordenação. Promover para shared lib em Fase 2.II.
 */
public record CalculoExecutadoEventDto(
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
}
