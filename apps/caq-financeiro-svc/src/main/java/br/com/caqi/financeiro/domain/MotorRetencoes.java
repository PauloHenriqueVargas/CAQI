package br.com.caqi.financeiro.domain;

import br.com.caqi.financeiro.api.dto.RetencaoDtos.ItemRetencaoDto;
import br.com.caqi.financeiro.api.dto.RetencaoDtos.RequisicaoRetencaoDto;
import br.com.caqi.financeiro.api.dto.RetencaoDtos.ResultadoRetencaoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de retenções tributárias na fonte (federal + municipal) para pagamentos
 * a fornecedores de serviços. Aplica regras simplificadas alinhadas a:
 *
 *   - Lei 9.430/1996, IN RFB 1.234/2012 (IRRF, PIS/COFINS/CSLL retenção fonte)
 *   - Lei 8.212/1991 + Decreto 3.048/1999 (INSS retenção 11%)
 *   - Lei Complementar 123/2006 + Resolução CGSN 140/2018 (Simples Nacional)
 *   - Legislação ISS de cada município (alíquota recebida na requisição)
 *
 * REGRAS SIMPLIFICADAS (MVP — confirmar com fiscal antes de prod):
 *
 *   Fornecedor OPTANTE do Simples Nacional:
 *     - Em geral, NÃO sofre IRRF / INSS / PIS / COFINS / CSLL retidos na fonte
 *       (recolhimento via DAS unificado). Exceções (PJ com cessão de mão-de-obra
 *       em condições específicas) NÃO tratadas neste MVP.
 *     - SOFRE ISS retido pelo município conforme legislação local.
 *
 *   Fornecedor NÃO-optante (lucro presumido/real, PJ):
 *     - IRRF: 1,5% sobre o valor bruto (serviços em geral, art. 647 RIR)
 *     - INSS: 11% APENAS para serviços com cessão de mão-de-obra
 *       (LIMPEZA_CONSERVACAO, ENGENHARIA, etc.) — NÃO para serviços
 *       intelectuais sem cessão (consultoria pontual)
 *     - PIS+COFINS+CSLL: 4,65% somados (1,65% + 3,0% + 1,0% = 4,65%) para
 *       serviços com NF-e quando valor > R$ 215,05 (limite IN 1.234)
 *     - ISS: conforme alíquota municipal informada
 */
@Service
@Slf4j
public class MotorRetencoes {

    // Alíquotas (%) — alinhadas à legislação federal vigente (revisar anualmente)
    private static final BigDecimal ALIQ_IRRF       = bd("1.5");
    private static final BigDecimal ALIQ_INSS       = bd("11.0");
    private static final BigDecimal ALIQ_PIS        = bd("0.65");
    private static final BigDecimal ALIQ_COFINS     = bd("3.0");
    private static final BigDecimal ALIQ_CSLL       = bd("1.0");
    private static final BigDecimal LIMITE_PCC      = bd("215.05");  // IN 1.234/2012

    // Tipos de serviço com cessão de mão-de-obra → INSS 11% retenção
    private static final List<String> SERVICOS_CESSAO_MO = List.of(
            "LIMPEZA_CONSERVACAO", "ENGENHARIA", "VIGILANCIA", "TRANSPORTE_CARGAS",
            "MANUTENCAO_PREDIAL", "OBRAS_CIVIS"
    );

    public ResultadoRetencaoDto calcular(RequisicaoRetencaoDto req) {
        log.info("Cálculo retenções valorBruto={} optanteSimples={} tipoServico={}",
                req.valorBruto(), req.optanteSimples(), req.tipoServico());

        BigDecimal valor = req.valorBruto();
        List<ItemRetencaoDto> memoria = new ArrayList<>();

        BigDecimal irrf = BigDecimal.ZERO;
        BigDecimal inss = BigDecimal.ZERO;
        BigDecimal iss = BigDecimal.ZERO;
        BigDecimal pis = BigDecimal.ZERO;
        BigDecimal cofins = BigDecimal.ZERO;
        BigDecimal csll = BigDecimal.ZERO;
        BigDecimal das = BigDecimal.ZERO;

        if (Boolean.TRUE.equals(req.optanteSimples())) {
            // Simples: SÓ ISS é retido na fonte (regra geral); demais são via DAS
            iss = aplicarIss(valor, req, memoria, "Simples Nacional — apenas ISS na fonte (LC 123/2006)");
            memoria.add(naoAplicado("IRRF", "Simples Nacional — recolhido via DAS"));
            memoria.add(naoAplicado("INSS", "Simples Nacional — recolhido via DAS"));
            memoria.add(naoAplicado("PIS",  "Simples Nacional — recolhido via DAS"));
            memoria.add(naoAplicado("COFINS","Simples Nacional — recolhido via DAS"));
            memoria.add(naoAplicado("CSLL", "Simples Nacional — recolhido via DAS"));
        } else {
            // Não-optante: regime cumulativo/lucro presumido
            irrf = aplicar(ALIQ_IRRF, valor, "IRRF",
                    "Lei 9.430/1996 — Serviços profissionais 1,5%", memoria);

            String tipo = (req.tipoServico() != null) ? req.tipoServico().toUpperCase() : "GERAL";
            if (SERVICOS_CESSAO_MO.contains(tipo)) {
                inss = aplicar(ALIQ_INSS, valor, "INSS",
                        "Lei 8.212/1991 — Cessão de mão-de-obra 11% (" + tipo + ")", memoria);
            } else {
                memoria.add(naoAplicado("INSS", "Serviço " + tipo + " sem cessão de mão-de-obra"));
            }

            if (valor.compareTo(LIMITE_PCC) > 0) {
                pis = aplicar(ALIQ_PIS, valor, "PIS", "IN RFB 1.234/2012", memoria);
                cofins = aplicar(ALIQ_COFINS, valor, "COFINS", "IN RFB 1.234/2012", memoria);
                csll = aplicar(ALIQ_CSLL, valor, "CSLL", "IN RFB 1.234/2012", memoria);
            } else {
                memoria.add(naoAplicado("PIS+COFINS+CSLL",
                        "Valor bruto ≤ R$ 215,05 — abaixo do limite de retenção (IN 1.234)"));
            }

            iss = aplicarIss(valor, req, memoria, "Lei municipal — ISS na fonte");
        }

        BigDecimal totalRetido = irrf.add(inss).add(iss).add(pis).add(cofins).add(csll).add(das);
        BigDecimal valorLiquido = valor.subtract(totalRetido);

        return new ResultadoRetencaoDto(valor, irrf, inss, iss, pis, cofins, csll, das,
                totalRetido, valorLiquido, memoria);
    }

    private static BigDecimal aplicar(BigDecimal aliquota, BigDecimal base, String tributo,
                                      String baseLegal, List<ItemRetencaoDto> memoria) {
        BigDecimal valor = base.multiply(aliquota).divide(bd("100"), 2, RoundingMode.HALF_UP);
        memoria.add(new ItemRetencaoDto(tributo, aliquota, base, valor, baseLegal, null));
        return valor;
    }

    private static BigDecimal aplicarIss(BigDecimal base, RequisicaoRetencaoDto req,
                                         List<ItemRetencaoDto> memoria, String contexto) {
        if (req.aliquotaIssMunicipal() == null || req.aliquotaIssMunicipal().signum() == 0) {
            memoria.add(naoAplicado("ISS", "aliquotaIssMunicipal=0 ou nula"));
            return BigDecimal.ZERO;
        }
        return aplicar(req.aliquotaIssMunicipal(), base, "ISS", contexto, memoria);
    }

    private static ItemRetencaoDto naoAplicado(String tributo, String motivo) {
        return new ItemRetencaoDto(tributo, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, motivo);
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
