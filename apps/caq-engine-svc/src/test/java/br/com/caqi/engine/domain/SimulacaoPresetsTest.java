package br.com.caqi.engine.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulacaoPresets — catálogo de cenários para CACS-Fundeb/CME")
class SimulacaoPresetsTest {

    @Test
    @DisplayName("Todos os presets têm nome único, base legal não-vazia e ao menos um override")
    void presets_validos() {
        var todos = SimulacaoPresets.TODOS;
        assertThat(todos).hasSize(3);

        long nomesUnicos = todos.stream().map(SimulacaoPreset::nome).distinct().count();
        assertThat(nomesUnicos).isEqualTo(3);

        for (var p : todos) {
            assertThat(p.nome()).isNotBlank();
            assertThat(p.titulo()).isNotBlank();
            assertThat(p.descricao()).isNotBlank();
            assertThat(p.impactoEsperado()).isNotBlank();
            assertThat(p.baseLegal()).isNotEmpty();
            assertThat(p.etapasRecomendadas()).isNotEmpty();

            boolean temOverride = !p.alunosPorTurma().isEmpty()
                    || !p.qtdPadraoInsumos().isEmpty()
                    || !p.custoMultiplierInsumos().isEmpty();
            assertThat(temOverride)
                    .as("preset %s deve ter ao menos um override", p.nome())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("'tempo_integral_universal' multiplica PES-001 ×1.50, SER/MAN ×1.20, MAT ×1.10")
    void tempoIntegral_multiplicadoresCorretos() {
        var p = SimulacaoPresets.TEMPO_INTEGRAL;
        assertThat(p.nome()).isEqualTo("tempo_integral_universal");
        assertThat(p.custoMultiplierInsumos())
                .containsEntry("PES-001", new BigDecimal("1.50"))
                .containsEntry("SER-001", new BigDecimal("1.20"))
                .containsEntry("MAN-001", new BigDecimal("1.20"))
                .containsEntry("MAT-001", new BigDecimal("1.10"));
        assertThat(p.alunosPorTurma()).isEmpty();
        assertThat(p.qtdPadraoInsumos()).isEmpty();
        assertThat(p.etapasRecomendadas()).contains("CRECHE", "PRE", "EF1", "EF2", "EM");
    }

    @Test
    @DisplayName("'menos_5_alunos_turma_ef' fixa EF1=20 e EF2=20, sem outros overrides")
    void menos5alunos_overridesCorretos() {
        var p = SimulacaoPresets.MENOS_5_ALUNOS_TURMA_EF;
        assertThat(p.alunosPorTurma())
                .containsEntry("EF1", new BigDecimal("20"))
                .containsEntry("EF2", new BigDecimal("20"))
                .hasSize(2);
        assertThat(p.qtdPadraoInsumos()).isEmpty();
        assertThat(p.custoMultiplierInsumos()).isEmpty();
        assertThat(p.etapasRecomendadas()).containsExactly("EF1", "EF2");
    }

    @Test
    @DisplayName("'reforco_creche_pre' dobra PES-001 (qtdPadrao=2) só em CRECHE/PRE")
    void reforcoCreche_overridesCorretos() {
        var p = SimulacaoPresets.REFORCO_CRECHE_PRE;
        assertThat(p.qtdPadraoInsumos()).containsEntry("PES-001", new BigDecimal("2"));
        assertThat(p.alunosPorTurma()).isEmpty();
        assertThat(p.custoMultiplierInsumos()).isEmpty();
        assertThat(p.etapasRecomendadas()).containsExactly("CRECHE", "PRE");
    }

    @Test
    @DisplayName("porNome() devolve preset existente; nome inválido devolve Optional.empty()")
    void porNome_lookup() {
        assertThat(SimulacaoPresets.porNome("tempo_integral_universal"))
                .isPresent()
                .get()
                .extracting(SimulacaoPreset::titulo)
                .isEqualTo("Tempo integral universal");

        assertThat(SimulacaoPresets.porNome("inexistente")).isEmpty();
        assertThat(SimulacaoPresets.porNome(null)).isEmpty();
        assertThat(SimulacaoPresets.porNome("")).isEmpty();
    }

    @Test
    @DisplayName("Base legal de cada preset cita as referências esperadas (sanity check)")
    void baseLegal_referenciasEsperadas() {
        assertThat(SimulacaoPresets.TEMPO_INTEGRAL.baseLegal())
                .anyMatch(s -> s.contains("PNE meta 6"))
                .anyMatch(s -> s.contains("LDB"));
        assertThat(SimulacaoPresets.MENOS_5_ALUNOS_TURMA_EF.baseLegal())
                .anyMatch(s -> s.contains("LDB art. 25"));
        assertThat(SimulacaoPresets.REFORCO_CRECHE_PRE.baseLegal())
                .anyMatch(s -> s.contains("Resolução CNE/CEB 5/2009"))
                .anyMatch(s -> s.contains("PNE meta 1"));
    }
}
