package br.com.caqi.financeiro.domain;

import br.com.caqi.financeiro.api.dto.ExecucaoFundebDto;
import br.com.caqi.financeiro.domain.repo.DespesaRepository;
import br.com.caqi.financeiro.domain.repo.ReceitaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calcula a execução das vinculações constitucionais e legais para um exercício:
 *   - MDE 25%   (CF/88 art. 212): despesas educação ÷ receitas de impostos
 *   - Fundeb 70% (Lei 14.113/2020): despesas de pessoal Fundeb ÷ receitas Fundeb
 *   - VAAT 15%  (Lei 14.113/2020): despesas de capital VAAT ÷ receitas VAAT
 *
 * NOTA Fase 3.B: o consumer compliance-svc receberá o evento de cálculo e gerará
 * alertas/bloqueios quando os percentuais ficarem abaixo do mínimo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FundebService {

    private static final BigDecimal MDE_MINIMO   = new BigDecimal("25.00");
    private static final BigDecimal FUNDEB_MINIMO = new BigDecimal("70.00");
    private static final BigDecimal VAAT_MINIMO   = new BigDecimal("15.00");

    private static final List<String> ORIGENS_MDE_RECEITA = List.of("impostos", "transferencias");
    private static final List<String> ORIGENS_FUNDEB      = List.of("Fundeb_VAAF", "Fundeb_VAAT", "Fundeb_VAAR");
    private static final List<String> ORIGENS_VAAT        = List.of("Fundeb_VAAT");
    private static final List<String> FONTES_FUNDEB       = List.of("VAAF", "VAAT", "VAAR");
    private static final List<String> FONTES_VAAT         = List.of("VAAT");

    private final ReceitaRepository receitaRepo;
    private final DespesaRepository despesaRepo;

    @Transactional(readOnly = true)
    public ExecucaoFundebDto calcular(int ano) {
        String inicio = String.format("%04d01", ano);
        String fim    = String.format("%04d12", ano);

        BigDecimal recImpostos = receitaRepo.somarPorOrigemNoAno(inicio, fim, ORIGENS_MDE_RECEITA);
        BigDecimal recFundeb   = receitaRepo.somarPorOrigemNoAno(inicio, fim, ORIGENS_FUNDEB);
        BigDecimal recVaat     = receitaRepo.somarPorOrigemNoAno(inicio, fim, ORIGENS_VAAT);

        BigDecimal despMde            = despesaRepo.somarPorSiopeGrupoNoAno(inicio, fim, "MDE");
        BigDecimal despPessoalFundeb  = despesaRepo.somarPessoalPorFonteNoAno(inicio, fim, FONTES_FUNDEB);
        BigDecimal despCapitalVaat    = despesaRepo.somarCapitalPorFonteNoAno(inicio, fim, FONTES_VAAT);

        BigDecimal pctMde      = pct(despMde, recImpostos);
        BigDecimal pctFundeb70 = pct(despPessoalFundeb, recFundeb);
        BigDecimal pctVaat15   = pct(despCapitalVaat, recVaat);

        log.info("Execução Fundeb ano={} MDE={}% Fundeb70={}% VAAT15={}%",
                ano, pctMde, pctFundeb70, pctVaat15);

        return new ExecucaoFundebDto(
                ano,
                recImpostos, recFundeb, recVaat,
                despMde, despPessoalFundeb, despCapitalVaat,
                pctMde, pctFundeb70, pctVaat15,
                pctMde.compareTo(MDE_MINIMO) >= 0,
                pctFundeb70.compareTo(FUNDEB_MINIMO) >= 0,
                pctVaat15.compareTo(VAAT_MINIMO) >= 0
        );
    }

    static BigDecimal pct(BigDecimal numerador, BigDecimal denominador) {
        if (denominador == null || denominador.signum() == 0) return BigDecimal.ZERO;
        return numerador.multiply(BigDecimal.valueOf(100))
                .divide(denominador, 2, RoundingMode.HALF_UP);
    }
}
