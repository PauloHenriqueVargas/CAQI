package br.com.caqi.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class SimulacaoDtos {

    private SimulacaoDtos() {}

    @Schema(name = "CenarioSimulacaoDto",
            description = "Cenário hipotético: aplica overrides ao cálculo CAQ/CAQi sem persistir")
    public record CenarioSimulacaoDto(
            @NotNull @Positive Integer ano,
            @NotEmpty List<String> escolas,
            @NotEmpty List<String> etapas,
            @Schema(description = "Override do parametro_etapa.alunos_por_turma — chave=etapaCodigo (ex.: EF1=20)")
            Map<String, BigDecimal> alunosPorTurma,
            @Schema(description = "Override do insumo.qtd_padrao — chave=insumoCodigo (ex.: PES-001=2 dobra a quantidade de docentes)")
            Map<String, BigDecimal> qtdPadraoInsumos,
            @Schema(description = "Multiplicador do custo_unitario — chave=insumoCodigo (1.20=+20%, 0.85=−15%)")
            Map<String, BigDecimal> custoMultiplierInsumos
    ) {
    }

    @Schema(name = "DiferencaItemDto", description = "Comparação atual vs simulado por (escola, etapa)")
    public record DiferencaItemDto(
            String escolaId,
            String etapaCodigo,
            BigDecimal caqiAtual,
            BigDecimal caqiSimulado,
            BigDecimal deltaCaqi,
            BigDecimal caqAtual,
            BigDecimal caqSimulado,
            BigDecimal deltaCaq,
            @Schema(description = "Variação percentual do CAQi (ex.: 19.15 = +19,15%)")
            BigDecimal pctDeltaCaqi
    ) {
    }

    @Schema(name = "SimulacaoResultadoDto",
            description = "Resultado da simulação: cálculo atual + cálculo com overrides + lista de diferenças")
    public record SimulacaoResultadoDto(
            Integer ano,
            ResultadoCalculoDto atual,
            ResultadoCalculoDto simulado,
            List<DiferencaItemDto> diferencas
    ) {
    }
}
