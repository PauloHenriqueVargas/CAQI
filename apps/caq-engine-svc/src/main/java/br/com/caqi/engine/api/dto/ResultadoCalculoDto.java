package br.com.caqi.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado consolidado de um cálculo CAQ/CAQi com memória de cálculo auditável")
public record ResultadoCalculoDto(
        Integer ano,
        List<ItemResultadoDto> itens,
        @Schema(description = "Memória item-a-item — uma linha por (escola, etapa, insumo) aplicado")
        List<ItemMemoriaCalculoDto> memoria
) {
}
