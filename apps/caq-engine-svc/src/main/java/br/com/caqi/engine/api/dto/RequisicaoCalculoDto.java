package br.com.caqi.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Requisição de cálculo CAQ/CAQi")
public record RequisicaoCalculoDto(
        @NotNull @Min(2020) @Schema(example = "2026") Integer ano,
        @NotEmpty @Schema(description = "IDs das escolas a calcular", example = "[\"ESC-001\"]") List<String> escolas,
        @NotEmpty @Schema(description = "Códigos de etapa", example = "[\"EF1\",\"EF2\"]") List<String> etapas,
        @Schema(description = "Se true, usa preços vigentes de contrato; se false, catálogo SINAPI/IPCA")
        Boolean usarPrecosVigentes
) {
}
