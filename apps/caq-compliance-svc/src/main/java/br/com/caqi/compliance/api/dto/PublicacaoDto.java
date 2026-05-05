package br.com.caqi.compliance.api.dto;

import br.com.caqi.compliance.domain.entity.PublicacaoPortal;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * Representação API de uma entrada de publicacao_portal.
 * Por padrão omite o snapshot (volume) — endpoint de detalhe inclui.
 */
public record PublicacaoDto(
        Long id,
        String tipo,
        String referencia,
        String conteudoHash,
        Long tamanhoBytes,
        String urlPublica,
        LocalDateTime dataPublicacao,
        JsonNode snapshot
) {
    public static PublicacaoDto fromResumo(PublicacaoPortal e) {
        return new PublicacaoDto(
                e.getId(), e.getTipo(), e.getReferencia(),
                e.getConteudoHash(), e.getTamanhoBytes(),
                e.getUrlPublica(), e.getDataPublicacao(), null);
    }

    public static PublicacaoDto fromDetalhe(PublicacaoPortal e) {
        return new PublicacaoDto(
                e.getId(), e.getTipo(), e.getReferencia(),
                e.getConteudoHash(), e.getTamanhoBytes(),
                e.getUrlPublica(), e.getDataPublicacao(), e.getSnapshot());
    }
}
