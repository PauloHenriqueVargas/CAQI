package br.com.caqi.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado consolidado de um cálculo CAQ/CAQi")
public record ResultadoCalculoDto(
        Integer ano,
        List<ItemResultadoDto> itens
) {
}
