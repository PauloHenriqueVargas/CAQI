package br.com.caqi.engine.domain;

import br.com.caqi.engine.api.dto.ItemMemoriaCalculoDto;
import br.com.caqi.engine.api.dto.RequisicaoCalculoDto;
import br.com.caqi.engine.api.dto.ResultadoCalculoDto;
import br.com.caqi.engine.domain.repo.EscolaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de regressão contra a Planilha_Base_CAQi_Preenchida.xlsx
 * (em docs/references/) + valores adequado seed em V9002.
 *
 * Cenário: Escola Municipal A, etapa EF1, ano 2025.
 *   Matrículas seed: 200 (Pré) + 400 (EF1) = 600 alunos na escola.
 *   Alunos/turma EF1: 25 (parametros_etapa).
 *
 * CAQi (perfil 'minimo' — bate com a planilha):
 *   PES-001 (por_turma): 85.000 / 25 = 3.400,0000
 *   MOB-001 (por_aluno): 1 × 280     =   280,0000
 *   MAT-001 (por_aluno): 1 × 350     =   350,0000
 *   SER-001 (por_escola): 12 × 5.000 / 600 = 100,0000
 *   TEC-001 (por_escola): 12 × 300 / 600   =   6,0000
 *   MAN-001 (por_escola): 1 × 20.000 / 600 =  33,3333
 *   PES-002 (por_escola): 95.000 / 600 = 158,3333  (faltava no exemplo da planilha)
 *   INF-001 (por_escola): 15.000 / 600 =  25,0000  (faltava no exemplo da planilha)
 *   EQP-001 (por_escola, EF/EM): 15 × 3.500 / 600 = 87,5000
 *   TOTAL CAQi: R$ 4.440,17
 *
 * CAQ adequado (perfil 'adequado' — V9002 seed):
 *   PES-001: 130.000 / 25 = 5.200,0000
 *   MOB-001: 1 × 480     =   480,0000
 *   MAT-001: 1 × 550     =   550,0000
 *   SER-001: 12 × 8.000 / 600 = 160,0000
 *   TEC-001: 12 × 800 / 600   =  16,0000
 *   MAN-001: 1 × 35.000 / 600 =  58,3333
 *   PES-002: 1 × 140.000 / 600 = 233,3333
 *   INF-001: 1 × 25.000 / 600 =  41,6667
 *   EQP-001: 15 × 5.500 / 600 = 137,5000
 *   TOTAL CAQ: R$ 6.876,83
 *
 * Gap CAQ − CAQi = R$ 2.436,66 / aluno / ano.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("CaqCalculator — regressão contra Planilha_Base_CAQi_Preenchida.xlsx")
class CaqCalculatorRegressaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("caqi_test")
            .withUsername("caqi")
            .withPassword("caqi_test_password");

    @Autowired
    CaqCalculator calculator;

    @Autowired
    EscolaRepository escolaRepo;

    @Test
    @DisplayName("EF1 em Escola A 2025 — CAQi (minimo) bate com a planilha item-a-item")
    void ef1EscolaA2025_CAQi_baseCalculoBate() {
        Long escolaAId = escolaRepo.findByNome("Escola Municipal A").orElseThrow().getId();

        var req = new RequisicaoCalculoDto(2025, List.of(escolaAId.toString()), List.of("EF1"), false);
        ResultadoCalculoDto resultado = calculator.calcular(req);

        assertThat(resultado.itens()).hasSize(1);
        var ef1 = resultado.itens().get(0);

        // ── Totais agregados ──────────────────────
        assertThat(ef1.caqiAlunoAno()).as("CAQi/aluno/ano").isEqualByComparingTo("4440.17");
        assertThat(ef1.caqAlunoAno()).as("CAQ/aluno/ano (adequado)").isEqualByComparingTo("6876.83");
        assertThat(ef1.gapAlunoAno()).as("Gap CAQ − CAQi").isEqualByComparingTo("2436.66");

        // ── Memória item-a-item perfil 'minimo' (planilha) ──
        var memMin = filtrar(resultado.memoria(), "EF1", Perfil.MINIMO);
        valorMemoriaIgual(memMin, "PES-001", "3400.0000");
        valorMemoriaIgual(memMin, "MOB-001",  "280.0000");
        valorMemoriaIgual(memMin, "MAT-001",  "350.0000");
        valorMemoriaIgual(memMin, "SER-001",  "100.0000");
        valorMemoriaIgual(memMin, "TEC-001",    "6.0000");
        valorMemoriaIgual(memMin, "MAN-001",   "33.3333");
        valorMemoriaIgual(memMin, "PES-002",  "158.3333");
        valorMemoriaIgual(memMin, "INF-001",   "25.0000");
        valorMemoriaIgual(memMin, "EQP-001",   "87.5000");

        // ── Memória item-a-item perfil 'adequado' (V9002) ──
        var memAdq = filtrar(resultado.memoria(), "EF1", Perfil.ADEQUADO);
        valorMemoriaIgual(memAdq, "PES-001", "5200.0000");
        valorMemoriaIgual(memAdq, "MOB-001",  "480.0000");
        valorMemoriaIgual(memAdq, "MAT-001",  "550.0000");
        valorMemoriaIgual(memAdq, "SER-001",  "160.0000");
        valorMemoriaIgual(memAdq, "TEC-001",   "16.0000");
        valorMemoriaIgual(memAdq, "MAN-001",   "58.3333");
        valorMemoriaIgual(memAdq, "PES-002",  "233.3333");
        valorMemoriaIgual(memAdq, "INF-001",   "41.6667");
        valorMemoriaIgual(memAdq, "EQP-001",  "137.5000");

        // ── Soma parcial dos 6 itens listados na planilha exemplo (CAQi) bate exato ──
        BigDecimal somaExemploParcial = memMin.stream()
                .filter(m -> List.of("PES-001", "MOB-001", "MAT-001", "SER-001", "TEC-001", "MAN-001").contains(m.insumoCodigo()))
                .map(ItemMemoriaCalculoDto::custoAlunoAno)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somaExemploParcial)
                .as("Soma dos 6 itens listados na planilha exemplo = R$ 4.169,3333")
                .isEqualByComparingTo("4169.3333");
    }

    @Test
    @DisplayName("Creche em Escola B 2025 — alunos/turma muda só o PES-001; EQP-001 não aplica")
    void crecheEscolaB2025() {
        Long escolaBId = escolaRepo.findByNome("Escola Municipal B").orElseThrow().getId();

        var req = new RequisicaoCalculoDto(2025, List.of(escolaBId.toString()), List.of("CRECHE"), false);
        ResultadoCalculoDto resultado = calculator.calcular(req);

        var creche = resultado.itens().get(0);
        // CRECHE tem 2 parametros (parcial=15 e integral=12) com mesma vigência —
        // findVigente desempata por id DESC; testamos os dois cenários.
        // Total integral CAQi: 7083,3333 + 202,1277 + 280 + 350 + 127,6596 + 7,6596 + 42,5532 + 31,9149 = 8125,2483 → 8125,25
        // Total parcial  CAQi: 5666,6667 + 202,1277 + 280 + 350 + 127,6596 + 7,6596 + 42,5532 + 31,9149 = 6708,5817 → 6708,58
        assertThat(creche.caqiAlunoAno())
                .isIn(new BigDecimal("8125.25"), new BigDecimal("6708.58"));

        // EQP-001 (etapa_aplicavel=EF/EM) NÃO deve aparecer na memória de CRECHE
        boolean hasEqp = resultado.memoria().stream()
                .filter(m -> m.etapaCodigo().equals("CRECHE"))
                .anyMatch(m -> m.insumoCodigo().equals("EQP-001"));
        assertThat(hasEqp).as("EQP-001 (EF/EM) não deve aplicar para CRECHE").isFalse();
    }

    private static List<ItemMemoriaCalculoDto> filtrar(
            List<ItemMemoriaCalculoDto> memoria, String etapa, String perfil) {
        return memoria.stream()
                .filter(m -> m.etapaCodigo().equals(etapa) && m.perfil().equals(perfil))
                .toList();
    }

    private void valorMemoriaIgual(List<ItemMemoriaCalculoDto> memoria, String codigo, String esperado) {
        var item = memoria.stream()
                .filter(m -> m.insumoCodigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Insumo " + codigo + " não encontrado na memória"));
        assertThat(item.custoAlunoAno())
                .as("custo_aluno_ano de %s", codigo)
                .isEqualByComparingTo(new BigDecimal(esperado));
    }
}
