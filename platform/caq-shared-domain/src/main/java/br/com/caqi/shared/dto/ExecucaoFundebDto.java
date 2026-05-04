package br.com.caqi.shared.dto;

import java.math.BigDecimal;

/**
 * Resposta de caq-financeiro-svc /api/v1/fundeb/execucao?ano=N.
 * Consumido por caq-compliance-svc (FinanceiroClient) — também serve como
 * contract documentado para qualquer cliente externo (BFF, painéis).
 */
public record ExecucaoFundebDto(
        Integer ano,
        BigDecimal receitasImpostos,
        BigDecimal receitasFundeb,
        BigDecimal receitasVaat,
        BigDecimal despesasMde,
        BigDecimal despesasPessoalFundeb,
        BigDecimal despesasCapitalVaat,
        BigDecimal pctMde,
        BigDecimal pctFundebPessoal,
        BigDecimal pctVaatCapital,
        Boolean cumpreMde,
        Boolean cumpreFundebPessoal,
        Boolean cumpreVaatCapital
) {
}
