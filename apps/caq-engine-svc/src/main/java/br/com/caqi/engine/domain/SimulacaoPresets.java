package br.com.caqi.engine.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo dos cenários típicos de simulação para uso por gestores e
 * conselhos de controle social (CACS-Fundeb, CME).
 *
 * Cada preset reflete uma política educacional concreta com base legal
 * própria. Os multiplicadores escolhidos são aproximações pedagógicas —
 * análise rigorosa requer customização dos parâmetros reais (CenarioSimulacaoDto).
 */
public final class SimulacaoPresets {

    /**
     * "Tempo integral universal" — jornada estendida (8h diárias).
     *  - PES-001 (Professor) ×1.50: jornada 7h → 10.5h equivalente
     *  - SER-001 (Limpeza) ×1.20: turno adicional
     *  - MAN-001 (Manutenção) ×1.20: uso intensivo da infraestrutura
     *  - MAT-001 (Material didático) ×1.10: oficinas/atividades complementares
     */
    public static final SimulacaoPreset TEMPO_INTEGRAL = new SimulacaoPreset(
            "tempo_integral_universal",
            "Tempo integral universal",
            "Estende a jornada escolar para tempo integral (≥7h diárias) em todas as etapas " +
            "da educação básica municipal. Multiplica custos de pessoal docente, serviços de " +
            "limpeza, manutenção predial e material didático para refletir a maior intensidade " +
            "de uso e horas-aula.",
            "+50% PES-001 (jornada estendida); +20% SER-001 e MAN-001; +10% MAT-001. " +
            "CAQ por aluno deve subir entre 25% e 40% conforme composição da escola.",
            List.of("CF/88 art. 206 IX", "PNE meta 6", "LDB art. 34 §2°", "Lei 14.113/2020"),
            Map.of(),
            Map.of(),
            Map.of(
                    "PES-001", new BigDecimal("1.50"),
                    "SER-001", new BigDecimal("1.20"),
                    "MAN-001", new BigDecimal("1.20"),
                    "MAT-001", new BigDecimal("1.10")
            ),
            List.of("CRECHE", "PRE", "EF1", "EF2", "EM")
    );

    /**
     * "Redução de alunos/turma EF" — 25 → 20 alunos/turma em EF1+EF2.
     *  - PES-001 é por_turma → custo total fica igual mas dividido por menos alunos
     *  - CAQ por aluno sobe ~25% no PES-001
     *  - Demais insumos não mudam (são por_aluno ou por_escola)
     */
    public static final SimulacaoPreset MENOS_5_ALUNOS_TURMA_EF = new SimulacaoPreset(
            "menos_5_alunos_turma_ef",
            "Redução de alunos por turma no Ensino Fundamental",
            "Reduz o tamanho médio das turmas de Ensino Fundamental (anos iniciais e finais) " +
            "de 25 para 20 alunos. Não altera o custo total da escola — apenas redistribui o " +
            "custo do professor (PES-001 é por_turma) por menos alunos, elevando o CAQ por aluno.",
            "Sem aumento de despesa no agregado. CAQ por aluno do EF1 e EF2 sobe ~25% pois " +
            "o salário do professor é distribuído por 20 alunos em vez de 25.",
            List.of("LDB art. 25", "PNE meta 7.5", "Lei 14.113/2020 art. 12"),
            Map.of(
                    "EF1", new BigDecimal("20"),
                    "EF2", new BigDecimal("20")
            ),
            Map.of(),
            Map.of(),
            List.of("EF1", "EF2")
    );

    /**
     * "Reforço creche/pré (auxiliar pedagógico)" — 2 docentes/turma em educação infantil.
     *  - PES-001 qtdPadrao: 1 → 2 (auxiliar de creche/pré-escola)
     *  - Resolução CNE/CEB 5/2009 e DCNs orientam ratio 1:6 (creche) e 1:25 (pré)
     */
    public static final SimulacaoPreset REFORCO_CRECHE_PRE = new SimulacaoPreset(
            "reforco_creche_pre",
            "Reforço creche/pré-escola (2º docente / auxiliar)",
            "Aplica o padrão de 2 docentes por turma em creche e pré-escola — professor + " +
            "auxiliar pedagógico — alinhado às DCNs e Resolução CNE/CEB 5/2009 (ratio docente/criança " +
            "1:6 em creche, 1:25 em pré).",
            "+100% PES-001 (dobra o custo de pessoal docente nas turmas atingidas). Não " +
            "afeta EF/EM (etapas filtradas).",
            List.of("LDB art. 29-31", "PNE meta 1", "Resolução CNE/CEB 5/2009 art. 8°", "DCN Educação Infantil"),
            Map.of(),
            Map.of("PES-001", new BigDecimal("2")),
            Map.of(),
            List.of("CRECHE", "PRE")
    );

    public static final List<SimulacaoPreset> TODOS = List.of(
            TEMPO_INTEGRAL,
            MENOS_5_ALUNOS_TURMA_EF,
            REFORCO_CRECHE_PRE
    );

    public static Optional<SimulacaoPreset> porNome(String nome) {
        if (nome == null) return Optional.empty();
        return TODOS.stream().filter(p -> p.nome().equals(nome)).findFirst();
    }

    private SimulacaoPresets() {}
}
