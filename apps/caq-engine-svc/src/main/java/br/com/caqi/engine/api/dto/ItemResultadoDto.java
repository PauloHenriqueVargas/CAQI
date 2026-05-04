package br.com.caqi.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Um item do resultado: par escola+etapa com R$/aluno/ano")
public record ItemResultadoDto(
        String escolaId,
        String etapaId,
        @Schema(description = "CAQi — padrão mínimo de qualidade") BigDecimal caqiAlunoAno,
        @Schema(description = "CAQ — padrão adequado de qualidade") BigDecimal caqAlunoAno,
        @Schema(description = "Gap de financiamento por aluno/ano (CAQ - executado)") BigDecimal gapAlunoAno
) {
}
