package br.com.caqi.engine.domain;

import br.com.caqi.engine.domain.entity.CustoInsumo;
import br.com.caqi.engine.domain.entity.IndicePreco;
import br.com.caqi.engine.domain.repo.IndicePrecoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Teste unitário (sem Spring) do IndexadorService.
 * Mockita IndicePrecoRepository para isolar a lógica de fator acumulado.
 */
class IndexadorServiceTest {

    @Test
    @DisplayName("Sem indice_atualizacao no custo → retorna valor original")
    void semIndice_retornaOriginal() {
        IndicePrecoRepository repo = mock(IndicePrecoRepository.class);
        IndexadorService svc = new IndexadorService(repo);

        CustoInsumo custo = custo(new BigDecimal("1000.00"), LocalDate.parse("2024-01-01"), null);
        BigDecimal r = svc.custoAtualizado(custo, LocalDate.parse("2025-01-01"));

        assertThat(r).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Vigência cobre data de referência → sem reajuste")
    void vigenciaCobreData_retornaOriginal() {
        IndicePrecoRepository repo = mock(IndicePrecoRepository.class);
        IndexadorService svc = new IndexadorService(repo);

        CustoInsumo custo = custo(new BigDecimal("1000.00"), LocalDate.parse("2025-06-01"), "IPCA");
        BigDecimal r = svc.custoAtualizado(custo, LocalDate.parse("2025-01-01"));

        assertThat(r).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("12 meses de IPCA acumulado: aplica produto (1 + var/100)")
    void doceMesesIpca_acumula() {
        IndicePrecoRepository repo = mock(IndicePrecoRepository.class);
        // 12 variações de 0,5% cada → fator = 1.005^12 ≈ 1.0617
        when(repo.findVariacoes(eq("IPCA"), any(), any())).thenReturn(List.of(
                ipca("2024-02", "0.5"), ipca("2024-03", "0.5"), ipca("2024-04", "0.5"),
                ipca("2024-05", "0.5"), ipca("2024-06", "0.5"), ipca("2024-07", "0.5"),
                ipca("2024-08", "0.5"), ipca("2024-09", "0.5"), ipca("2024-10", "0.5"),
                ipca("2024-11", "0.5"), ipca("2024-12", "0.5"), ipca("2025-01", "0.5")
        ));
        IndexadorService svc = new IndexadorService(repo);

        // Vigência 2024-01-01, dataReferencia 2025-01-31 → acumula 2024-02..2025-01 (12 meses)
        CustoInsumo custo = custo(new BigDecimal("1000.00"), LocalDate.parse("2024-01-01"), "IPCA");
        BigDecimal r = svc.custoAtualizado(custo, LocalDate.parse("2025-01-31"));

        // 1.005^12 = 1.06167781...; × 1000 = 1061.67781 → arredondado HALF_UP scale 2 = 1061.68
        assertThat(r).isEqualByComparingTo("1061.68");
    }

    @Test
    @DisplayName("IPCA negativo (deflação) reduz o custo")
    void ipcaNegativo_reduz() {
        IndicePrecoRepository repo = mock(IndicePrecoRepository.class);
        when(repo.findVariacoes(eq("IPCA"), any(), any())).thenReturn(List.of(
                ipca("2024-02", "-1.0"), ipca("2024-03", "-1.0")
        ));
        IndexadorService svc = new IndexadorService(repo);

        CustoInsumo custo = custo(new BigDecimal("1000.00"), LocalDate.parse("2024-01-01"), "IPCA");
        BigDecimal r = svc.custoAtualizado(custo, LocalDate.parse("2024-03-15"));

        // 0.99 × 0.99 = 0.9801; × 1000 = 980.10
        assertThat(r).isEqualByComparingTo("980.10");
    }

    @Test
    @DisplayName("Sem variações cadastradas no intervalo → retorna original com warn")
    void semVariacoes_retornaOriginal() {
        IndicePrecoRepository repo = mock(IndicePrecoRepository.class);
        when(repo.findVariacoes(any(), any(), any())).thenReturn(List.of());
        IndexadorService svc = new IndexadorService(repo);

        CustoInsumo custo = custo(new BigDecimal("1000.00"), LocalDate.parse("2024-01-01"), "IPCA");
        BigDecimal r = svc.custoAtualizado(custo, LocalDate.parse("2025-01-01"));

        assertThat(r).isEqualByComparingTo("1000.00");
    }

    private CustoInsumo custo(BigDecimal valor, LocalDate vigencia, String indice) {
        CustoInsumo c = new CustoInsumo();
        c.setCustoUnitario(valor);
        c.setVigenciaInicio(vigencia);
        c.setIndiceAtualizacao(indice);
        return c;
    }

    private IndicePreco ipca(String competencia, String valor) {
        IndicePreco i = new IndicePreco();
        i.setNome("IPCA");
        i.setCompetencia(competencia);
        i.setValor(new BigDecimal(valor));
        return i;
    }
}
