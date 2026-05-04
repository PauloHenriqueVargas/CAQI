package br.com.caqi.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Linha da memória de cálculo: 1 insumo aplicado para 1 (escola, etapa, perfil). */
@Schema(description = "Detalhe item-a-item de como cada insumo contribui para o R$/aluno/ano")
public record ItemMemoriaCalculoDto(
        String escolaId,
        String etapaCodigo,
        @Schema(description = "minimo (compõe CAQi) | adequado (compõe CAQ)") String perfil,
        String insumoCodigo,
        String insumoNome,
        @Schema(description = "por_aluno | por_turma | por_escola") String tipoAplicacao,
        BigDecimal qtdAplicada,
        BigDecimal custoUnitario,
        @Schema(description = "qtd × custo_unitario (custo total anualizado do insumo)") BigDecimal custoAnual,
        @Schema(description = "Divisor: 1, alunos_por_turma ou total_alunos_escola") BigDecimal divisor,
        @Schema(description = "custo_anual / divisor") BigDecimal custoAlunoAno,
        @Schema(description = "Explicação textual auditável") String baseCalculo
) {
}
