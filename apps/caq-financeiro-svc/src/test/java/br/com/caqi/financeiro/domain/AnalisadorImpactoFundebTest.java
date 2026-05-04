package br.com.caqi.financeiro.domain;

import br.com.caqi.shared.dto.ExecucaoFundebDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AnalisadorImpactoFundebTest {

    private final AnalisadorImpactoFundeb analisador = new AnalisadorImpactoFundeb();

    @Test
    @DisplayName("Sem transição negativa → Optional.empty")
    void semTransicao_naoDetecta() {
        var antes  = exec(true,  true,  true,  bd("100"), bd("80"), bd("30"));
        var depois = exec(true,  true,  true,  bd("90"),  bd("75"), bd("25"));
        assertThat(analisador.detectarTransicaoNegativa(antes, depois)).isEmpty();
    }

    @Test
    @DisplayName("Quando MDE cai de TRUE para FALSE → detecta com texto descritivo")
    void mdeDerrubado_detecta() {
        var antes  = exec(true,  true,  true,  bd("30"), bd("80"), bd("30"));
        var depois = exec(false, true,  true,  bd("20"), bd("80"), bd("30"));
        var motivo = analisador.detectarTransicaoNegativa(antes, depois);
        assertThat(motivo).isPresent();
        assertThat(motivo.get()).contains("MDE").contains("20").contains("CF/88");
    }

    @Test
    @DisplayName("Quando Fundeb 70% cai → detecta")
    void fundebDerrubado_detecta() {
        var antes  = exec(true, true,  true, bd("100"), bd("75"), bd("30"));
        var depois = exec(true, false, true, bd("100"), bd("60"), bd("30"));
        var motivo = analisador.detectarTransicaoNegativa(antes, depois);
        assertThat(motivo).isPresent();
        assertThat(motivo.get()).contains("Fundeb").contains("60").contains("70%");
    }

    @Test
    @DisplayName("Quando VAAT 15% cai → detecta")
    void vaatDerrubado_detecta() {
        var antes  = exec(true, true, true,  bd("100"), bd("80"), bd("20"));
        var depois = exec(true, true, false, bd("100"), bd("80"), bd("10"));
        var motivo = analisador.detectarTransicaoNegativa(antes, depois);
        assertThat(motivo).isPresent();
        assertThat(motivo.get()).contains("VAAT").contains("15%");
    }

    @Test
    @DisplayName("Se já estava em violação (FALSE→FALSE) — NÃO detecta (a despesa não causou)")
    void jaViolavaAntes_naoDetecta() {
        var antes  = exec(false, true, true, bd("20"), bd("80"), bd("30"));
        var depois = exec(false, true, true, bd("18"), bd("80"), bd("30"));
        assertThat(analisador.detectarTransicaoNegativa(antes, depois)).isEmpty();
    }

    private static ExecucaoFundebDto exec(
            boolean cumpreMde, boolean cumpreFundeb, boolean cumpreVaat,
            BigDecimal pctMde, BigDecimal pctFundeb, BigDecimal pctVaat) {
        return new ExecucaoFundebDto(2025,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                pctMde, pctFundeb, pctVaat,
                cumpreMde, cumpreFundeb, cumpreVaat);
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
