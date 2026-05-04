package br.com.caqi.financeiro.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class SiopeDtos {

    private SiopeDtos() {}

    @Schema(name = "SiopeExportDto",
            description = "Quadro consolidado SIOPE-ready para o exercício. Pode ser convertido para CSV/XLS para " +
                    "upload manual no portal FNDE (não há API pública para envio automático em 2025).")
    public record SiopeExportDto(
            Integer ano,
            String tenantMunicipioId,
            Instant geradoEm,
            ReceitasResumo receitas,
            DespesasResumo despesas,
            Vinculacoes vinculacoes,
            @Schema(description = "Pendências detectadas — devem ser corrigidas antes de enviar ao SIOPE")
            List<SiopePendenciaDto> pendencias
    ) {
    }

    @Schema(name = "ReceitasResumo")
    public record ReceitasResumo(
            @Schema(description = "Total por origem (impostos, transferencias, Fundeb_VAAF, etc.)")
            Map<String, BigDecimal> porOrigem,
            BigDecimal total
    ) {
    }

    @Schema(name = "DespesasResumo")
    public record DespesasResumo(
            Map<String, BigDecimal> porSiopeGrupo,
            Map<String, BigDecimal> porFonteRecurso,
            @Schema(description = "Agrupamento por classe (pessoal=3.1.x; capital=4.x; demais=outras)")
            Map<String, BigDecimal> porClasse,
            BigDecimal totalMde,
            BigDecimal total
    ) {
    }

    @Schema(name = "Vinculacoes", description = "Status legal das 3 vinculações principais")
    public record Vinculacoes(
            VinculacaoDetalhe mde25,
            VinculacaoDetalhe fundeb70,
            VinculacaoDetalhe vaat15
    ) {
    }

    @Schema(name = "VinculacaoDetalhe")
    public record VinculacaoDetalhe(
            BigDecimal executado,
            BigDecimal minimo,
            BigDecimal gapPp,
            Boolean cumpre,
            String baseLegal
    ) {
    }

    @Schema(name = "SiopePendenciaDto",
            description = "Inconsistência que impede envio limpo ao SIOPE")
    public record SiopePendenciaDto(
            @Schema(description = "RECEITA | DESPESA | CONTRATO") String tipoEntidade,
            Long entidadeId,
            @Schema(description = "info | warn | alta | critica") String severidade,
            String descricao
    ) {
    }
}
