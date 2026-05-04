package br.com.caqi.compliance.api.dto;

import java.math.BigDecimal;

/**
 * Mirror do DTO publicado por caq-financeiro-svc /api/v1/fundeb/execucao.
 * Cuidado: mantém field names alinhados — qualquer mudança no produtor exige
 * coordenação. Idealmente promover para uma library compartilhada (Fase 2.II).
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
