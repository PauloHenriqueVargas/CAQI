package br.com.caqi.escolar.censo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset do layout INEP MATRICULA_*.csv (microdados Censo Escolar — aluno-level).
 *
 * Layout oficial: <a href="https://www.gov.br/inep/pt-br/acesso-a-informacao/dados-abertos/microdados/censo-escolar">INEP — Microdados</a>
 * Codificação Latin-1 (ISO-8859-1), separador `;`.
 *
 * <h3>LGPD</h3>
 * Esta classe é um DTO técnico de leitura. O ID_ALUNO é pseudonimizado
 * antes de persistir; data de nascimento é descartada (mantemos só
 * NU_IDADE_REFERENCIA agregada).
 *
 * <h3>TP_ETAPA_ENSINO</h3>
 * Código numérico INEP (1–77) que indica a etapa em que a matrícula está.
 * Mapeado para CRECHE/PRE/EF1/EF2/EM/EJA/PROF via {@link CensoEtapaEnsinoMapper}.
 */
public record CensoMatriculaRecord(
        @JsonProperty("NU_ANO_CENSO") Integer nuAnoCenso,
        @JsonProperty("ID_MATRICULA") String idMatricula,
        @JsonProperty("ID_ALUNO") String idAluno,
        @JsonProperty("CO_ENTIDADE") String coEntidade,
        @JsonProperty("CO_MUNICIPIO") String coMunicipio,
        @JsonProperty("TP_DEPENDENCIA_ADM") Integer tpDependenciaAdm,
        @JsonProperty("TP_ETAPA_ENSINO") Integer tpEtapaEnsino,
        @JsonProperty("NU_IDADE_REFERENCIA") Integer nuIdadeReferencia,
        @JsonProperty("TP_SEXO") Integer tpSexo,
        @JsonProperty("TP_COR_RACA") Integer tpCorRaca,
        @JsonProperty("TP_ZONA_RESIDENCIAL") Integer tpZonaResidencial,
        @JsonProperty("IN_NECESSIDADE_ESPECIAL") Integer inNecessidadeEspecial,
        @JsonProperty("IN_CEGUEIRA") Integer inCegueira,
        @JsonProperty("IN_BAIXA_VISAO") Integer inBaixaVisao,
        @JsonProperty("IN_SURDEZ") Integer inSurdez,
        @JsonProperty("IN_DEFICIENCIA_AUDITIVA") Integer inDeficienciaAuditiva,
        @JsonProperty("IN_DEFICIENCIA_FISICA") Integer inDeficienciaFisica,
        @JsonProperty("IN_DEFICIENCIA_INTELECTUAL") Integer inDeficienciaIntelectual,
        @JsonProperty("IN_DEFICIENCIA_MULTIPLA") Integer inDeficienciaMultipla,
        @JsonProperty("IN_AUTISMO") Integer inAutismo,
        @JsonProperty("IN_ALTAS_HABILIDADES") Integer inAltasHabilidades
) {
    public boolean ehDependenciaMunicipal() {
        return tpDependenciaAdm != null && tpDependenciaAdm == 3;
    }

    public boolean inNecessidadeEspecialBool() {
        return inNecessidadeEspecial != null && inNecessidadeEspecial == 1;
    }

    /**
     * Concatena os flags IN_* habilitados em um CSV legível.
     * Ex.: "CEGUEIRA,DEFICIENCIA_INTELECTUAL".
     */
    public String necessidadesCsv() {
        StringBuilder sb = new StringBuilder();
        appendIfFlag(sb, inCegueira,                "CEGUEIRA");
        appendIfFlag(sb, inBaixaVisao,              "BAIXA_VISAO");
        appendIfFlag(sb, inSurdez,                  "SURDEZ");
        appendIfFlag(sb, inDeficienciaAuditiva,     "DEFICIENCIA_AUDITIVA");
        appendIfFlag(sb, inDeficienciaFisica,       "DEFICIENCIA_FISICA");
        appendIfFlag(sb, inDeficienciaIntelectual,  "DEFICIENCIA_INTELECTUAL");
        appendIfFlag(sb, inDeficienciaMultipla,     "DEFICIENCIA_MULTIPLA");
        appendIfFlag(sb, inAutismo,                 "AUTISMO");
        appendIfFlag(sb, inAltasHabilidades,        "ALTAS_HABILIDADES");
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void appendIfFlag(StringBuilder sb, Integer flag, String label) {
        if (flag != null && flag == 1) {
            if (sb.length() > 0) sb.append(',');
            sb.append(label);
        }
    }
}
