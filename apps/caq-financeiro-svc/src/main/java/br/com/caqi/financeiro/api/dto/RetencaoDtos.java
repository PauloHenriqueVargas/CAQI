package br.com.caqi.financeiro.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class RetencaoDtos {

    private RetencaoDtos() {}

    @Schema(name = "RequisicaoRetencaoDto",
            description = "Solicitação de cálculo de retenções tributárias na fonte para um pagamento")
    public record RequisicaoRetencaoDto(
            @NotNull @Positive
            @Schema(description = "Valor bruto da nota/empenho (R$)") BigDecimal valorBruto,

            @Schema(description = "CNPJ do fornecedor — opcional, apenas referência") String cnpj,

            @NotNull
            @Schema(description = "Se true, fornecedor é optante do Simples Nacional (LC 123/2006). " +
                    "Sofre apenas ISS retido (em geral) — IRRF/INSS/PIS/COFINS/CSLL não são retidos.")
            Boolean optanteSimples,

            @Schema(description = "Natureza da despesa (PCASP) — ex.: 3.3.90.39",
                    example = "3.3.90.39") String naturezaDespesa,

            @Schema(description = "Tipo do serviço — GERAL | MEDICOS | ENGENHARIA | TRANSPORTE | LIMPEZA_CONSERVACAO. " +
                    "Afeta INSS (cessão de mão-de-obra) e IRRF.",
                    example = "GERAL", defaultValue = "GERAL") String tipoServico,

            @DecimalMin("0.0") @DecimalMax("10.0")
            @Schema(description = "Alíquota ISS municipal (%) — varia por município (2-5%)",
                    example = "5.0") BigDecimal aliquotaIssMunicipal,

            @Schema(description = "Tipo de serviço para fins de IRRF — se permite isenção/alíquotas especiais")
            Boolean isencaoIrrf
    ) {
    }

    @Schema(name = "ResultadoRetencaoDto", description = "Retenções calculadas + memória de cálculo")
    public record ResultadoRetencaoDto(
            BigDecimal valorBruto,
            BigDecimal irrf,
            BigDecimal inss,
            BigDecimal iss,
            BigDecimal pis,
            BigDecimal cofins,
            BigDecimal csll,
            BigDecimal das,
            BigDecimal totalRetido,
            BigDecimal valorLiquido,
            @Schema(description = "Memória item-a-item (cada componente com base e alíquota)")
            List<ItemRetencaoDto> memoria
    ) {
    }

    @Schema(name = "ItemRetencaoDto",
            description = "Detalhe de um componente da retenção (base, alíquota, valor, justificativa legal)")
    public record ItemRetencaoDto(
            String tributo,           // IRRF | INSS | ISS | PIS | COFINS | CSLL | DAS
            BigDecimal aliquota,      // %
            BigDecimal base,
            BigDecimal valor,
            String baseLegal,
            String observacao
    ) {
    }
}
