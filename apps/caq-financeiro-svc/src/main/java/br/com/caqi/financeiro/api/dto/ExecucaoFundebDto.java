package br.com.caqi.financeiro.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Execução consolidada das vinculações Fundeb/MDE/VAAT para um exercício")
public record ExecucaoFundebDto(
        Integer ano,
        @Schema(description = "Receitas de impostos + transferências (base do MDE)")
        BigDecimal receitasImpostos,
        @Schema(description = "Receitas Fundeb (VAAF + VAAT + VAAR)")
        BigDecimal receitasFundeb,
        @Schema(description = "Receitas VAAT (subset do Fundeb)")
        BigDecimal receitasVaat,
        @Schema(description = "Despesas marcadas como MDE")
        BigDecimal despesasMde,
        @Schema(description = "Despesas de pessoal (3.1.x) financiadas pelo Fundeb")
        BigDecimal despesasPessoalFundeb,
        @Schema(description = "Despesas de capital (4.x) financiadas pela VAAT")
        BigDecimal despesasCapitalVaat,
        @Schema(description = "% executado MDE — mínimo 25% (CF/88 art. 212)")
        BigDecimal pctMde,
        @Schema(description = "% executado Fundeb pessoal — mínimo 70% (Lei 14.113/2020)")
        BigDecimal pctFundebPessoal,
        @Schema(description = "% executado VAAT capital — mínimo 15% (Lei 14.113/2020)")
        BigDecimal pctVaatCapital,
        @Schema(description = "Cumpre MDE 25%?") Boolean cumpreMde,
        @Schema(description = "Cumpre Fundeb 70%?") Boolean cumpreFundebPessoal,
        @Schema(description = "Cumpre VAAT 15%?") Boolean cumpreVaatCapital
) {
}
