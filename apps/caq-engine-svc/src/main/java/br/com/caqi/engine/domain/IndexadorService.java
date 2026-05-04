package br.com.caqi.engine.domain;

import br.com.caqi.engine.domain.entity.CustoInsumo;
import br.com.caqi.engine.domain.entity.IndicePreco;
import br.com.caqi.engine.domain.repo.IndicePrecoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Aplica reajuste pelo índice associado ao custo (IPCA, SINAPI...) acumulado entre
 * o mês imediatamente posterior à vigência do custo e a data de referência do cálculo.
 *
 * Fórmula:
 *   custo_atualizado = custo_unitario × ∏ (1 + variacao_mensal_%/100)
 *
 * Casos:
 *   - sem indice_atualizacao no custo → retorna custo original
 *   - vigencia ≥ dataReferencia (custo já cobre o período) → retorna custo original
 *   - sem variações cadastradas no intervalo → retorna custo original (com warn)
 *   - reajustes encontrados → multiplica
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexadorService {

    private final IndicePrecoRepository indiceRepo;

    public BigDecimal custoAtualizado(CustoInsumo custo, LocalDate dataReferencia) {
        if (custo.getIndiceAtualizacao() == null || custo.getIndiceAtualizacao().isBlank()) {
            return custo.getCustoUnitario();
        }
        if (!custo.getVigenciaInicio().isBefore(dataReferencia)) {
            // vigência cobre a data de referência — sem reajuste a aplicar
            return custo.getCustoUnitario();
        }

        // Acumula a partir do MÊS SEGUINTE à vigência (a vigência já reflete o preço daquele mês)
        YearMonth inicio = YearMonth.from(custo.getVigenciaInicio()).plusMonths(1);
        YearMonth fim = YearMonth.from(dataReferencia);
        if (fim.isBefore(inicio)) {
            return custo.getCustoUnitario();
        }

        List<IndicePreco> variacoes = indiceRepo.findVariacoes(
                custo.getIndiceAtualizacao(), inicio.toString(), fim.toString());
        if (variacoes.isEmpty()) {
            log.warn("Custo {} indexado por {} sem variações entre {} e {} — usando valor original",
                    custo.getId(), custo.getIndiceAtualizacao(), inicio, fim);
            return custo.getCustoUnitario();
        }

        BigDecimal fator = BigDecimal.ONE;
        for (IndicePreco i : variacoes) {
            // (1 + variacao_%/100)
            BigDecimal mult = BigDecimal.ONE.add(i.getValor().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
            fator = fator.multiply(mult);
        }
        return custo.getCustoUnitario().multiply(fator).setScale(2, RoundingMode.HALF_UP);
    }
}
