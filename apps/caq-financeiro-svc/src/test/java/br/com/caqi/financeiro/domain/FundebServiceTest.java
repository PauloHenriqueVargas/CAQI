package br.com.caqi.financeiro.domain;

import br.com.caqi.shared.dto.ExecucaoFundebDto;
import br.com.caqi.financeiro.domain.repo.DespesaRepository;
import br.com.caqi.financeiro.domain.repo.ReceitaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Teste unitário (Mockito, sem Spring) do FundebService.
 * Cenário-base reflete o seed financeiro V9003 — todos os 3 limites são cumpridos.
 */
class FundebServiceTest {

    @Test
    @DisplayName("Cenário do seed V9003: cumpre os 3 limites legais (MDE 25%, Fundeb 70%, VAAT 15%)")
    void cenarioSeed_cumpreTodosLimites() {
        ReceitaRepository receitaRepo = mock(ReceitaRepository.class);
        DespesaRepository despesaRepo = mock(DespesaRepository.class);

        // Receitas (do seed V9003 — ano 2025)
        // ORIGENS_MDE_RECEITA = impostos + transferencias = 100k + 80k = 180k
        // ORIGENS_FUNDEB     = VAAF + VAAT + VAAR        = 200k + 50k + 30k = 280k
        // ORIGENS_VAAT       = VAAT                      = 50k
        when(receitaRepo.somarPorOrigemNoAno(any(), any(), eqList("impostos", "transferencias")))
                .thenReturn(new BigDecimal("180000"));
        when(receitaRepo.somarPorOrigemNoAno(any(), any(), eqList("Fundeb_VAAF", "Fundeb_VAAT", "Fundeb_VAAR")))
                .thenReturn(new BigDecimal("280000"));
        when(receitaRepo.somarPorOrigemNoAno(any(), any(), eqList("Fundeb_VAAT")))
                .thenReturn(new BigDecimal("50000"));

        // Despesas
        // MDE: D1+D2+D3+D4+D5+D6 = 200+30+20+15+10+5 = 280k
        // Pessoal Fundeb: D1 (200k pessoal VAAF) + D2 (30k pessoal VAAR) = 230k
        // Capital VAAT:   D5 (10k equip) + D6 (5k obras) = 15k
        when(despesaRepo.somarPorSiopeGrupoNoAno(any(), any(), any())).thenReturn(new BigDecimal("280000"));
        when(despesaRepo.somarPessoalPorFonteNoAno(any(), any(), any())).thenReturn(new BigDecimal("230000"));
        when(despesaRepo.somarCapitalPorFonteNoAno(any(), any(), any())).thenReturn(new BigDecimal("15000"));

        FundebService svc = new FundebService(receitaRepo, despesaRepo);
        ExecucaoFundebDto r = svc.calcular(2025);

        assertThat(r.ano()).isEqualTo(2025);

        // 280.000 / 180.000 = 155,56% → cumpre MDE 25%
        assertThat(r.pctMde()).isEqualByComparingTo("155.56");
        assertThat(r.cumpreMde()).isTrue();

        // 230.000 / 280.000 = 82,14% → cumpre Fundeb 70%
        assertThat(r.pctFundebPessoal()).isEqualByComparingTo("82.14");
        assertThat(r.cumpreFundebPessoal()).isTrue();

        // 15.000 / 50.000 = 30,00% → cumpre VAAT 15%
        assertThat(r.pctVaatCapital()).isEqualByComparingTo("30.00");
        assertThat(r.cumpreVaatCapital()).isTrue();
    }

    @Test
    @DisplayName("Quando 70% Fundeb é violado, cumpreFundebPessoal=false")
    void violaFundeb70_marcaFalse() {
        ReceitaRepository receitaRepo = mock(ReceitaRepository.class);
        DespesaRepository despesaRepo = mock(DespesaRepository.class);

        when(receitaRepo.somarPorOrigemNoAno(any(), any(), anyList())).thenReturn(new BigDecimal("100000"));
        when(despesaRepo.somarPorSiopeGrupoNoAno(any(), any(), anyString())).thenReturn(new BigDecimal("100000"));
        // Pessoal Fundeb = 50k, receitas Fundeb = 100k → 50% < 70%
        when(despesaRepo.somarPessoalPorFonteNoAno(any(), any(), anyList())).thenReturn(new BigDecimal("50000"));
        when(despesaRepo.somarCapitalPorFonteNoAno(any(), any(), anyList())).thenReturn(new BigDecimal("20000"));

        FundebService svc = new FundebService(receitaRepo, despesaRepo);
        ExecucaoFundebDto r = svc.calcular(2025);

        assertThat(r.pctFundebPessoal()).isEqualByComparingTo("50.00");
        assertThat(r.cumpreFundebPessoal()).isFalse();
    }

    @Test
    @DisplayName("Receita zerada não causa divisão por zero — devolve 0%")
    void receitaZero_naoDividePorZero() {
        ReceitaRepository receitaRepo = mock(ReceitaRepository.class);
        DespesaRepository despesaRepo = mock(DespesaRepository.class);
        when(receitaRepo.somarPorOrigemNoAno(any(), any(), anyList())).thenReturn(BigDecimal.ZERO);
        when(despesaRepo.somarPorSiopeGrupoNoAno(any(), any(), anyString())).thenReturn(new BigDecimal("100"));
        when(despesaRepo.somarPessoalPorFonteNoAno(any(), any(), anyList())).thenReturn(BigDecimal.ZERO);
        when(despesaRepo.somarCapitalPorFonteNoAno(any(), any(), anyList())).thenReturn(BigDecimal.ZERO);

        FundebService svc = new FundebService(receitaRepo, despesaRepo);
        ExecucaoFundebDto r = svc.calcular(2025);

        assertThat(r.pctMde()).isEqualByComparingTo("0");
        assertThat(r.pctFundebPessoal()).isEqualByComparingTo("0");
        assertThat(r.pctVaatCapital()).isEqualByComparingTo("0");
    }

    private static List<String> eqList(String... values) {
        return org.mockito.ArgumentMatchers.eq(List.of(values));
    }
}
