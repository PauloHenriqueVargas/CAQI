package br.com.caqi.financeiro.domain;

import br.com.caqi.financeiro.api.dto.RetencaoDtos.ItemRetencaoDto;
import br.com.caqi.financeiro.api.dto.RetencaoDtos.RequisicaoRetencaoDto;
import br.com.caqi.financeiro.api.dto.RetencaoDtos.ResultadoRetencaoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MotorRetencoesTest {

    private final MotorRetencoes motor = new MotorRetencoes();

    @Test
    @DisplayName("Não-optante, serviço GERAL R$ 10.000 → IRRF 1,5% + PIS+COFINS+CSLL 4,65% + ISS 5%")
    void naoOptanteServicoGeral() {
        var req = new RequisicaoRetencaoDto(
                bd("10000"), "00.000.000/0001-00", false,
                "3.3.90.39", "GERAL", bd("5.0"), false);

        ResultadoRetencaoDto r = motor.calcular(req);

        assertThat(r.valorBruto()).isEqualByComparingTo("10000");
        assertThat(r.irrf()).isEqualByComparingTo("150.00");      // 1.5% × 10.000
        assertThat(r.inss()).isEqualByComparingTo("0");           // GERAL não tem cessão MO
        assertThat(r.pis()).isEqualByComparingTo("65.00");        // 0.65%
        assertThat(r.cofins()).isEqualByComparingTo("300.00");    // 3.0%
        assertThat(r.csll()).isEqualByComparingTo("100.00");      // 1.0%
        assertThat(r.iss()).isEqualByComparingTo("500.00");       // 5%
        assertThat(r.das()).isEqualByComparingTo("0");
        assertThat(r.totalRetido()).isEqualByComparingTo("1115.00");
        assertThat(r.valorLiquido()).isEqualByComparingTo("8885.00");
    }

    @Test
    @DisplayName("Não-optante LIMPEZA_CONSERVACAO R$ 10.000 → INSS 11% adicional")
    void naoOptanteServicoComCessaoMaoDeObra() {
        var req = new RequisicaoRetencaoDto(
                bd("10000"), null, false,
                "3.3.90.39", "LIMPEZA_CONSERVACAO", bd("5.0"), false);

        ResultadoRetencaoDto r = motor.calcular(req);

        assertThat(r.inss()).isEqualByComparingTo("1100.00");     // 11% × 10.000
        assertThat(r.totalRetido()).isEqualByComparingTo("2215.00"); // 150 + 1100 + 65 + 300 + 100 + 500
        assertThat(r.valorLiquido()).isEqualByComparingTo("7785.00");
    }

    @Test
    @DisplayName("Optante Simples Nacional: SÓ ISS retido")
    void optanteSimples_apenasIss() {
        var req = new RequisicaoRetencaoDto(
                bd("10000"), "00.000.000/0001-00", true,
                "3.3.90.39", "GERAL", bd("5.0"), false);

        ResultadoRetencaoDto r = motor.calcular(req);

        assertThat(r.irrf()).isEqualByComparingTo("0");
        assertThat(r.inss()).isEqualByComparingTo("0");
        assertThat(r.pis()).isEqualByComparingTo("0");
        assertThat(r.cofins()).isEqualByComparingTo("0");
        assertThat(r.csll()).isEqualByComparingTo("0");
        assertThat(r.iss()).isEqualByComparingTo("500.00");
        assertThat(r.totalRetido()).isEqualByComparingTo("500.00");
        assertThat(r.valorLiquido()).isEqualByComparingTo("9500.00");

        // Memória mostra POR QUÊ os outros foram zero
        List<ItemRetencaoDto> nao = r.memoria().stream()
                .filter(i -> i.observacao() != null)
                .toList();
        assertThat(nao).extracting(ItemRetencaoDto::tributo)
                .contains("IRRF", "INSS", "PIS", "COFINS", "CSLL");
        assertThat(nao).allMatch(i -> i.observacao().contains("DAS"));
    }

    @Test
    @DisplayName("Valor pequeno (R$ 200) — abaixo do limite de PIS/COFINS/CSLL (R$ 215,05)")
    void valorAbaixoLimitePcc_naoRetemPisCofinsCsll() {
        var req = new RequisicaoRetencaoDto(
                bd("200"), null, false,
                "3.3.90.39", "GERAL", bd("5.0"), false);

        ResultadoRetencaoDto r = motor.calcular(req);

        assertThat(r.irrf()).isEqualByComparingTo("3.00");        // 1.5% × 200
        assertThat(r.pis()).isEqualByComparingTo("0");            // abaixo limite
        assertThat(r.cofins()).isEqualByComparingTo("0");
        assertThat(r.csll()).isEqualByComparingTo("0");
        assertThat(r.iss()).isEqualByComparingTo("10.00");        // 5% × 200
    }

    @Test
    @DisplayName("ISS zero quando alíquota municipal não é informada")
    void semAliquotaIss_naoRetem() {
        var req = new RequisicaoRetencaoDto(
                bd("10000"), null, false,
                "3.3.90.39", "GERAL", null, false);

        ResultadoRetencaoDto r = motor.calcular(req);
        assertThat(r.iss()).isEqualByComparingTo("0");
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
