package br.com.caqi.engine.domain;

import br.com.caqi.engine.api.dto.ItemResultadoDto;
import br.com.caqi.engine.api.dto.RequisicaoCalculoDto;
import br.com.caqi.engine.api.dto.ResultadoCalculoDto;
import br.com.caqi.engine.api.dto.SimulacaoDtos.CenarioSimulacaoDto;
import br.com.caqi.engine.api.dto.SimulacaoDtos.DiferencaItemDto;
import br.com.caqi.engine.api.dto.SimulacaoDtos.SimulacaoResultadoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulador "e se?" — recalcula CAQ/CAQi com overrides hipotéticos e devolve
 * o impacto comparativo. Não persiste nada.
 *
 * Cenários típicos:
 *   - Reduzir alunos_por_turma de EF1 (efeito sobre PES-001 e equivalentes por_turma)
 *   - Aumentar qtd_padrao de insumos (ex.: 2 docentes/turma para suporte)
 *   - Reajustar custo (ex.: piso salarial +25%)
 *   - Combinações dos 3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimuladorService {

    private final CaqCalculator calculator;

    @Transactional(readOnly = true)
    public SimulacaoResultadoDto simular(CenarioSimulacaoDto cenario) {
        log.info("Simulação ano={} escolas={} etapas={} overrides=({},{},{})",
                cenario.ano(), cenario.escolas().size(), cenario.etapas().size(),
                size(cenario.alunosPorTurma()), size(cenario.qtdPadraoInsumos()),
                size(cenario.custoMultiplierInsumos()));

        RequisicaoCalculoDto req = new RequisicaoCalculoDto(
                cenario.ano(), cenario.escolas(), cenario.etapas(), false);
        Overrides overrides = new Overrides(
                cenario.alunosPorTurma(),
                cenario.qtdPadraoInsumos(),
                cenario.custoMultiplierInsumos());

        ResultadoCalculoDto atual = calculator.calcular(req, Overrides.NONE);
        ResultadoCalculoDto simulado = calculator.calcular(req, overrides);

        List<DiferencaItemDto> diferencas = computarDiferencas(atual, simulado);
        return new SimulacaoResultadoDto(cenario.ano(), atual, simulado, diferencas);
    }

    private List<DiferencaItemDto> computarDiferencas(ResultadoCalculoDto atual, ResultadoCalculoDto simulado) {
        Map<String, ItemResultadoDto> simuladoIdx = new HashMap<>();
        for (ItemResultadoDto item : simulado.itens()) {
            simuladoIdx.put(chave(item), item);
        }

        List<DiferencaItemDto> result = new ArrayList<>();
        for (ItemResultadoDto a : atual.itens()) {
            ItemResultadoDto s = simuladoIdx.get(chave(a));
            if (s == null) continue;
            BigDecimal deltaCaqi = s.caqiAlunoAno().subtract(a.caqiAlunoAno());
            BigDecimal deltaCaq  = s.caqAlunoAno().subtract(a.caqAlunoAno());
            BigDecimal pctDeltaCaqi = (a.caqiAlunoAno().signum() == 0)
                    ? BigDecimal.ZERO
                    : deltaCaqi.multiply(BigDecimal.valueOf(100))
                            .divide(a.caqiAlunoAno(), 2, RoundingMode.HALF_UP);
            result.add(new DiferencaItemDto(
                    a.escolaId(), a.etapaId(),
                    a.caqiAlunoAno(), s.caqiAlunoAno(), deltaCaqi,
                    a.caqAlunoAno(), s.caqAlunoAno(), deltaCaq,
                    pctDeltaCaqi
            ));
        }
        return result;
    }

    private static String chave(ItemResultadoDto i) {
        return i.escolaId() + "|" + i.etapaId();
    }

    private static int size(Map<?, ?> m) {
        return (m == null) ? 0 : m.size();
    }
}
