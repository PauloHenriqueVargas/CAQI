package br.com.caqi.escolar.censo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Tradução dos campos QT_MAT_* do INEP para os códigos canônicos de Etapa
 * que o engine-svc usa em CaqCalculator/parametros.
 *
 * Códigos canônicos (V0001/V9001 do engine):
 *  - CRECHE  ← QT_MAT_INF_CRE
 *  - PRE     ← QT_MAT_INF_PRE
 *  - EF1     ← QT_MAT_FUND_AI    (anos iniciais 1°-5°)
 *  - EF2     ← QT_MAT_FUND_AF    (anos finais 6°-9°)
 *  - EM      ← QT_MAT_MED
 *  - EJA     ← QT_MAT_EJA
 *  - PROF    ← QT_MAT_PROF
 *
 * Os 4 primeiros entram no cálculo CAQi/CAQ municipal típico
 * (Lei 14.113/2020 art. 12 — Fundeb cobre creche até EM).
 */
public final class CensoEtapaMapper {

    public static final Map<String, Function<CensoEscolaRecord, Integer>> ETAPA_PARA_CAMPO = new LinkedHashMap<>();

    static {
        ETAPA_PARA_CAMPO.put("CRECHE", CensoEscolaRecord::qtMatInfCre);
        ETAPA_PARA_CAMPO.put("PRE",    CensoEscolaRecord::qtMatInfPre);
        ETAPA_PARA_CAMPO.put("EF1",    CensoEscolaRecord::qtMatFundAi);
        ETAPA_PARA_CAMPO.put("EF2",    CensoEscolaRecord::qtMatFundAf);
        ETAPA_PARA_CAMPO.put("EM",     CensoEscolaRecord::qtMatMed);
        ETAPA_PARA_CAMPO.put("EJA",    CensoEscolaRecord::qtMatEja);
        ETAPA_PARA_CAMPO.put("PROF",   CensoEscolaRecord::qtMatProf);
    }

    private CensoEtapaMapper() {}
}
