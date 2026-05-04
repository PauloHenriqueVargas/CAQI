package br.com.caqi.compliance.api.dto;

import br.com.caqi.compliance.domain.entity.Notificacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Notificação de violação ou alerta de compliance")
public record NotificacaoDto(
        Long id,
        String tipo,
        String severidade,
        String titulo,
        String descricao,
        Integer anoReferencia,
        String baseLegal,
        Map<String, Object> payload,
        String status,
        Instant createdAt,
        Instant resolvedAt
) {
    public static NotificacaoDto from(Notificacao n) {
        return new NotificacaoDto(n.getId(), n.getTipo(), n.getSeveridade(),
                n.getTitulo(), n.getDescricao(), n.getAnoReferencia(),
                n.getBaseLegal(), n.getPayload(), n.getStatus(),
                n.getCreatedAt(), n.getResolvedAt());
    }
}
