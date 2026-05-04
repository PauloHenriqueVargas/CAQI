package br.com.caqi.engine.domain;

import br.com.caqi.engine.api.dto.SimulacaoDtos.CenarioSimulacaoDto;
import br.com.caqi.engine.api.dto.SimulacaoDtos.DiferencaItemDto;
import br.com.caqi.engine.api.dto.SimulacaoDtos.SimulacaoResultadoDto;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integração do Simulador contra o seed (Escola A, EF1, 2025).
 *
 * Cenário base (sem overrides — Fase 2.B.i):
 *   CAQi = R$ 4.440,17  /  CAQ adequado = R$ 6.876,83  /  gap = R$ 2.436,66
 *
 * Override `alunosPorTurma EF1 = 20` (era 25):
 *   PES-001 perfil minimo: 85.000/20 = 4.250 (era 3.400) → +850
 *   PES-001 perfil adequado: 130.000/20 = 6.500 (era 5.200) → +1.300
 *   Demais insumos NÃO MUDAM (são por_aluno ou por_escola, não por_turma).
 *
 *   CAQi simulado = 4.440,17 + 850 = 5.290,17
 *   CAQ  simulado = 6.876,83 + 1.300 = 8.176,83
 *   Delta % CAQi = 850 / 4.440,17 × 100 = 19,14% (HALF_UP)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("SimuladorService — impacto de reduzir alunos/turma EF1 de 25 para 20")
class SimuladorServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("caqi_test")
            .withUsername("caqi")
            .withPassword("caqi_test_password");

    @Autowired
    SimuladorService simulador;

    @Autowired
    EscolaRepository escolaRepo;

    @Test
    void overrideAlunosPorTurma_afetaSomentePorTurma() {
        Long escolaA = escolaRepo.findByNome("Escola Municipal A").orElseThrow().getId();

        var cenario = new CenarioSimulacaoDto(
                2025,
                List.of(escolaA.toString()),
                List.of("EF1"),
                Map.of("EF1", new BigDecimal("20")),
                Map.of(),
                Map.of()
        );

        SimulacaoResultadoDto r = simulador.simular(cenario);

        // Atual (idem CaqCalculatorRegressaoTest)
        var atual = r.atual().itens().get(0);
        assertThat(atual.caqiAlunoAno()).isEqualByComparingTo("4440.17");
        assertThat(atual.caqAlunoAno()).isEqualByComparingTo("6876.83");

        // Simulado: PES-001 muda de 3400→4250 (CAQi) e 5200→6500 (CAQ); demais idem
        var simulado = r.simulado().itens().get(0);
        assertThat(simulado.caqiAlunoAno()).isEqualByComparingTo("5290.17");
        assertThat(simulado.caqAlunoAno()).isEqualByComparingTo("8176.83");

        // Diferenças
        assertThat(r.diferencas()).hasSize(1);
        DiferencaItemDto d = r.diferencas().get(0);
        assertThat(d.deltaCaqi()).isEqualByComparingTo("850.00");
        assertThat(d.deltaCaq()).isEqualByComparingTo("1300.00");
        assertThat(d.pctDeltaCaqi()).isEqualByComparingTo("19.14");

        // Verificar que PES-001 é o único insumo afetado (memória)
        var pesMin = r.simulado().memoria().stream()
                .filter(m -> m.insumoCodigo().equals("PES-001") && m.perfil().equals("minimo"))
                .findFirst().orElseThrow();
        assertThat(pesMin.custoAlunoAno()).isEqualByComparingTo("4250.0000");
        assertThat(pesMin.divisor()).isEqualByComparingTo("20");

        var mobMin = r.simulado().memoria().stream()
                .filter(m -> m.insumoCodigo().equals("MOB-001") && m.perfil().equals("minimo"))
                .findFirst().orElseThrow();
        assertThat(mobMin.custoAlunoAno()).isEqualByComparingTo("280.0000"); // inalterado
    }

    @Test
    @DisplayName("Multiplicador de custo: PES-001 +25% (piso salarial) — só pessoal sobe")
    void custoMultiplier_subePessoal() {
        Long escolaA = escolaRepo.findByNome("Escola Municipal A").orElseThrow().getId();

        var cenario = new CenarioSimulacaoDto(
                2025,
                List.of(escolaA.toString()),
                List.of("EF1"),
                Map.of(),
                Map.of(),
                Map.of("PES-001", new BigDecimal("1.25"))
        );

        SimulacaoResultadoDto r = simulador.simular(cenario);

        // PES-001 perfil minimo: 85000 × 1.25 / 25 = 4250 (era 3400) → +850 igual ao caso anterior
        // PES-001 perfil adequado: 130000 × 1.25 / 25 = 6500 (era 5200) → +1300
        var d = r.diferencas().get(0);
        assertThat(d.deltaCaqi()).isEqualByComparingTo("850.00");
        assertThat(d.deltaCaq()).isEqualByComparingTo("1300.00");
    }
}
