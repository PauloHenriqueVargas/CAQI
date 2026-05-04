package br.com.caqi.engine.api.dto;

import br.com.caqi.engine.domain.entity.CalculoCaq;
import br.com.caqi.engine.domain.entity.CalculoCaqItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public final class CalculoCaqDtos {

    private CalculoCaqDtos() {}

    @Schema(name = "CalculoCaqResumoDto", description = "Resumo de cálculo persistido (sem memória)")
    public record CalculoCaqResumoDto(
            Long id,
            Long escolaId,
            Long etapaId,
            Integer ano,
            BigDecimal valorCaqiAlunoAno,
            BigDecimal valorCaqAlunoAno,
            BigDecimal gapAdequado
    ) {
        public static CalculoCaqResumoDto from(CalculoCaq c) {
            BigDecimal gap = (c.getValorCaqAlunoAno() != null && c.getValorCaqiAlunoAno() != null)
                    ? c.getValorCaqAlunoAno().subtract(c.getValorCaqiAlunoAno())
                    : null;
            return new CalculoCaqResumoDto(c.getId(), c.getEscolaId(), c.getEtapaId(), c.getAno(),
                    c.getValorCaqiAlunoAno(), c.getValorCaqAlunoAno(), gap);
        }
    }

    @Schema(name = "CalculoCaqDetalheDto", description = "Cálculo persistido com memória item-a-item")
    public record CalculoCaqDetalheDto(
            Long id,
            Long escolaId,
            Long etapaId,
            Integer ano,
            BigDecimal valorCaqiAlunoAno,
            BigDecimal valorCaqAlunoAno,
            BigDecimal gapAdequado,
            List<CalculoCaqItemDto> itens
    ) {
        public static CalculoCaqDetalheDto from(CalculoCaq c) {
            BigDecimal gap = (c.getValorCaqAlunoAno() != null && c.getValorCaqiAlunoAno() != null)
                    ? c.getValorCaqAlunoAno().subtract(c.getValorCaqiAlunoAno())
                    : null;
            return new CalculoCaqDetalheDto(c.getId(), c.getEscolaId(), c.getEtapaId(), c.getAno(),
                    c.getValorCaqiAlunoAno(), c.getValorCaqAlunoAno(), gap,
                    c.getItens().stream().map(CalculoCaqItemDto::from).toList());
        }
    }

    @Schema(name = "CalculoCaqItemDto", description = "Linha persistida da memória de cálculo")
    public record CalculoCaqItemDto(
            Long id,
            String perfil,
            String insumoCodigo,
            String insumoNome,
            String tipoAplicacao,
            BigDecimal qtdAplicada,
            BigDecimal custoUnitario,
            BigDecimal custoAnual,
            BigDecimal divisor,
            BigDecimal custoAlunoAno,
            String baseCalculo
    ) {
        public static CalculoCaqItemDto from(CalculoCaqItem ci) {
            return new CalculoCaqItemDto(ci.getId(), ci.getPerfil(),
                    ci.getInsumoCodigo(), ci.getInsumoNome(), ci.getTipoAplicacao(),
                    ci.getQtdAplicada(), ci.getCustoUnitario(), ci.getCustoAnual(),
                    ci.getDivisor(), ci.getCustoAlunoAno(), ci.getBaseCalculo());
        }
    }
}
