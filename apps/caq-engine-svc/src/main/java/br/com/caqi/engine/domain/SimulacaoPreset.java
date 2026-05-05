package br.com.caqi.engine.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Receita pré-configurada de simulação "e se?".
 *
 * Um preset define o conjunto de overrides (alunos/turma, qtd padrão, custo)
 * + uma narrativa pedagógica (título, descrição, impacto esperado, base
 * legal) + as etapas onde faz sentido aplicar.
 *
 * O serviço consome o preset para construir um CenarioSimulacaoDto e roda
 * o simulador normal — não há lógica de cálculo nova.
 */
public record SimulacaoPreset(
        String nome,
        String titulo,
        String descricao,
        String impactoEsperado,
        List<String> baseLegal,
        Map<String, BigDecimal> alunosPorTurma,
        Map<String, BigDecimal> qtdPadraoInsumos,
        Map<String, BigDecimal> custoMultiplierInsumos,
        List<String> etapasRecomendadas
) {
}
