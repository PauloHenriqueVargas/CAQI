package br.com.caqi.financeiro.api.dto;

import br.com.caqi.financeiro.api.dto.RetencaoDtos.ResultadoRetencaoDto;
import br.com.caqi.financeiro.domain.entity.MedicaoContrato;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public final class MedicaoDtos {

    private MedicaoDtos() {}

    @Schema(name = "MedicaoDto")
    public record MedicaoDto(Long id, Long contratoId, String competencia,
                             BigDecimal valorMedido, String notaFiscal) {
        public static MedicaoDto from(MedicaoContrato m) {
            return new MedicaoDto(m.getId(), m.getContratoId(), m.getCompetencia(),
                    m.getValorMedido(), m.getNotaFiscal());
        }
    }

    @Schema(name = "MedicaoCreateDto",
            description = "Cria medição contratual. Se tipoServico+aliquotaIssMunicipal informados, " +
                    "computa preview de retenções junto (não persiste retenção; só simulação para emissão de guia).")
    public record MedicaoCreateDto(
            @NotBlank @Pattern(regexp = "\\d{6}", message = "competencia deve ser YYYYMM") String competencia,
            @NotNull @Positive BigDecimal valorMedido,
            String notaFiscal,

            @Schema(description = "Para preview de retenções — opcional", defaultValue = "GERAL")
            String tipoServico,
            @DecimalMin("0.0") @DecimalMax("10.0")
            @Schema(description = "Alíquota ISS municipal (%) — necessária para incluir ISS no preview")
            BigDecimal aliquotaIssMunicipal
    ) {
        public MedicaoContrato toEntity(Long contratoId) {
            MedicaoContrato m = new MedicaoContrato();
            m.setContratoId(contratoId);
            m.setCompetencia(competencia);
            m.setValorMedido(valorMedido);
            m.setNotaFiscal(notaFiscal);
            return m;
        }
    }

    @Schema(name = "MedicaoComRetencoesDto",
            description = "Resposta enriquecida da criação de medição: medição persistida + preview opcional de retenções")
    public record MedicaoComRetencoesDto(
            MedicaoDto medicao,
            ResultadoRetencaoDto retencoesPreview
    ) {
    }
}
