package br.com.caqi.engine.domain;

import br.com.caqi.engine.api.dto.ItemResultadoDto;
import br.com.caqi.engine.api.dto.RequisicaoCalculoDto;
import br.com.caqi.engine.api.dto.ResultadoCalculoDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Motor de cálculo CAQ/CAQi.
 *
 * Fase 1 (atual): stub — devolve estrutura correta com valores zerados, permite contrato OpenAPI navegável.
 *
 * Fase 2 (planejado, ver docs/ROADMAP.md):
 *   - Carregar parâmetros vigentes por etapa/modalidade (alunos/turma, carga horária, jornada).
 *   - Carregar catálogo de insumos com custo unitário vigente (SINAPI/IPCA ou contrato vigente).
 *   - Calcular matriz Insumo→Custo anualizada por escola, somar e dividir por matrículas.
 *   - Aplicar perfis: CAQi (padrão mínimo) e CAQ (padrão adequado).
 *   - Validar regras legais: Fundeb 70% pessoal, VAAT 15% capital, MDE 25%.
 *   - Persistir em tabela calculo_caq + memória de cálculo (linhas item-a-item).
 *   - Teste de regressão: comparar com docs/references/Planilha_Base_CAQi_Preenchida.xlsx (±0,5%).
 */
@Component
public class CaqCalculator {

    public ResultadoCalculoDto calcular(RequisicaoCalculoDto req) {
        List<ItemResultadoDto> itens = req.escolas().stream()
                .flatMap(escolaId -> req.etapas().stream()
                        .map(etapaId -> new ItemResultadoDto(
                                escolaId,
                                etapaId,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        )))
                .toList();
        return new ResultadoCalculoDto(req.ano(), itens);
    }
}
