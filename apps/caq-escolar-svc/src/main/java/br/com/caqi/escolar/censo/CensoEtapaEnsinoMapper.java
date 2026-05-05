package br.com.caqi.escolar.censo;

/**
 * Tradução do código numérico TP_ETAPA_ENSINO (INEP, 1–77) para os
 * códigos canônicos do engine CAQ (CRECHE/PRE/EF1/EF2/EM/EJA/PROF).
 *
 * Subset apenas das etapas relevantes para gestão municipal — etapas
 * técnicas/profissionais detalhadas (66-77) caem no genérico PROF;
 * códigos não mapeados retornam null e a matrícula é descartada.
 *
 * Referência: dicionário oficial dos microdados Censo Escolar.
 */
public final class CensoEtapaEnsinoMapper {

    /** @return código canônico da etapa, ou {@code null} se não relevante. */
    public static String mapear(Integer tpEtapaEnsino) {
        if (tpEtapaEnsino == null) return null;
        return switch (tpEtapaEnsino) {
            case 1 -> "CRECHE";                 // Educação Infantil — Creche
            case 2 -> "PRE";                    // Educação Infantil — Pré-escola

            // Ensino Fundamental — anos iniciais (1° ao 5°)
            case 4, 5, 6, 7, 14, 15, 16, 17, 18 -> "EF1";

            // Ensino Fundamental — anos finais (6° ao 9°)
            case 8, 9, 10, 11, 19, 20, 21, 41 -> "EF2";

            // Ensino Médio (1° ao 4° / integrado / normal)
            case 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38 -> "EM";

            // EJA — Educação de Jovens e Adultos
            case 65, 67, 69, 70, 71, 72, 73, 74 -> "EJA";

            // Educação Profissional (cursos técnicos integrados/concomitantes)
            case 39, 40, 64, 68 -> "PROF";

            default -> null;
        };
    }

    private CensoEtapaEnsinoMapper() {}
}
