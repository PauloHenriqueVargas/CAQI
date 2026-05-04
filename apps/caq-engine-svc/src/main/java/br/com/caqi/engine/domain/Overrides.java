package br.com.caqi.engine.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Sobreposições in-memory para o CaqCalculator — usadas por simulações
 * "e se?" sem persistir mudanças no banco.
 *
 * Cada mapa é opcional; valores ausentes caem nos dados reais do banco.
 *   - alunosPorTurmaPorEtapa: substitui o ParametroEtapa.alunosPorTurma
 *     da etapa indicada (chave = etapaCodigo)
 *   - qtdPadraoPorInsumo: substitui Insumo.qtdPadrao (chave = insumoCodigo)
 *   - custoMultiplierPorInsumo: multiplica CustoInsumo.custoUnitario por
 *     o fator informado (1.20 = +20%; 0.85 = −15%)
 */
public record Overrides(
        Map<String, BigDecimal> alunosPorTurmaPorEtapa,
        Map<String, BigDecimal> qtdPadraoPorInsumo,
        Map<String, BigDecimal> custoMultiplierPorInsumo
) {
    public static final Overrides NONE = new Overrides(Map.of(), Map.of(), Map.of());

    /** Construtor null-safe. */
    public Overrides {
        alunosPorTurmaPorEtapa = (alunosPorTurmaPorEtapa != null) ? alunosPorTurmaPorEtapa : Map.of();
        qtdPadraoPorInsumo     = (qtdPadraoPorInsumo     != null) ? qtdPadraoPorInsumo     : Map.of();
        custoMultiplierPorInsumo = (custoMultiplierPorInsumo != null) ? custoMultiplierPorInsumo : Map.of();
    }

    public Optional<BigDecimal> alunosPorTurma(String etapaCodigo) {
        return Optional.ofNullable(alunosPorTurmaPorEtapa.get(etapaCodigo));
    }

    public Optional<BigDecimal> qtdPadrao(String insumoCodigo) {
        return Optional.ofNullable(qtdPadraoPorInsumo.get(insumoCodigo));
    }

    public BigDecimal custoMultiplier(String insumoCodigo) {
        return custoMultiplierPorInsumo.getOrDefault(insumoCodigo, BigDecimal.ONE);
    }

    public boolean isEmpty() {
        return alunosPorTurmaPorEtapa.isEmpty() && qtdPadraoPorInsumo.isEmpty()
                && custoMultiplierPorInsumo.isEmpty();
    }
}
