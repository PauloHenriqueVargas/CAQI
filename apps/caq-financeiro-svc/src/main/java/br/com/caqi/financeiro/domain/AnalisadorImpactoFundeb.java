package br.com.caqi.financeiro.domain;

import br.com.caqi.shared.dto.ExecucaoFundebDto;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Compara duas execuções (antes/depois) e detecta se uma vinculação
 * passou de "cumpre" para "não cumpre" — sinal de que a operação que
 * gerou o "depois" derruba a regra. Pura — fácil de testar.
 */
@Component
public class AnalisadorImpactoFundeb {

    public Optional<String> detectarTransicaoNegativa(ExecucaoFundebDto antes, ExecucaoFundebDto depois) {
        if (caiu(antes.cumpreMde(), depois.cumpreMde())) {
            return Optional.of(String.format(
                    "MDE derrubado de %s%% para %s%% (mínimo 25%% — CF/88 art. 212)",
                    antes.pctMde(), depois.pctMde()));
        }
        if (caiu(antes.cumpreFundebPessoal(), depois.cumpreFundebPessoal())) {
            return Optional.of(String.format(
                    "Fundeb pessoal derrubado de %s%% para %s%% (mínimo 70%% — Lei 14.113/2020)",
                    antes.pctFundebPessoal(), depois.pctFundebPessoal()));
        }
        if (caiu(antes.cumpreVaatCapital(), depois.cumpreVaatCapital())) {
            return Optional.of(String.format(
                    "VAAT capital derrubado de %s%% para %s%% (mínimo 15%% — Lei 14.113/2020)",
                    antes.pctVaatCapital(), depois.pctVaatCapital()));
        }
        return Optional.empty();
    }

    private static boolean caiu(Boolean antes, Boolean depois) {
        return Boolean.TRUE.equals(antes) && Boolean.FALSE.equals(depois);
    }
}
