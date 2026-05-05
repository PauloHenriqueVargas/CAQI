package br.com.caqi.escolar.censo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset do layout INEP ESCOLAS_*.csv (microdados Censo Escolar).
 *
 * Layout oficial: <a href="https://www.gov.br/inep/pt-br/acesso-a-informacao/dados-abertos/microdados/censo-escolar">INEP — Microdados</a>.
 *
 * Codificação: Latin-1 (ISO-8859-1). Separador: ';'. Decimal: ','.
 *
 * Campos não usados pelo CAQi (CO_REGIAO, CO_MESORREGIAO, infraestrutura
 * detalhada, etc.) são ignorados via {@code @JsonIgnoreProperties(ignoreUnknown=true)}
 * configurado no schema do parser.
 *
 * Tipos de dependência (TP_DEPENDENCIA):
 *  1=Federal, 2=Estadual, 3=Municipal, 4=Privada.
 *
 * Tipos de localização (TP_LOCALIZACAO):
 *  1=Urbana, 2=Rural.
 *
 * Situação de funcionamento (TP_SITUACAO_FUNCIONAMENTO):
 *  1=Em atividade, 2=Paralisada, 3=Extinta no ano, 4=Extinta em anos anteriores.
 */
public record CensoEscolaRecord(
        @JsonProperty("NU_ANO_CENSO") Integer nuAnoCenso,
        @JsonProperty("CO_ENTIDADE") String coEntidade,
        @JsonProperty("NO_ENTIDADE") String noEntidade,
        @JsonProperty("CO_MUNICIPIO") String coMunicipio,
        @JsonProperty("CO_UF") String coUf,
        @JsonProperty("TP_DEPENDENCIA") Integer tpDependencia,
        @JsonProperty("TP_LOCALIZACAO") Integer tpLocalizacao,
        @JsonProperty("TP_SITUACAO_FUNCIONAMENTO") Integer tpSituacaoFuncionamento,
        @JsonProperty("QT_MAT_INF_CRE") Integer qtMatInfCre,
        @JsonProperty("QT_MAT_INF_PRE") Integer qtMatInfPre,
        @JsonProperty("QT_MAT_FUND_AI") Integer qtMatFundAi,
        @JsonProperty("QT_MAT_FUND_AF") Integer qtMatFundAf,
        @JsonProperty("QT_MAT_MED")     Integer qtMatMed,
        @JsonProperty("QT_MAT_EJA")     Integer qtMatEja,
        @JsonProperty("QT_MAT_PROF")    Integer qtMatProf
) {
    public boolean ehMunicipal() {
        return tpDependencia != null && tpDependencia == 3;
    }

    public boolean emAtividade() {
        return tpSituacaoFuncionamento != null && tpSituacaoFuncionamento == 1;
    }

    public String localizacaoTexto() {
        if (tpLocalizacao == null) return null;
        return tpLocalizacao == 1 ? "urbana" : tpLocalizacao == 2 ? "rural" : null;
    }
}
