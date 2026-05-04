package br.com.caqi.financeiro.api.dto;

import br.com.caqi.financeiro.domain.entity.Despesa;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public final class DespesaDtos {

    private DespesaDtos() {}

    @Schema(name = "DespesaDto")
    public record DespesaDto(
            Long id, String competencia, String natureza, BigDecimal valor,
            Long fonteRecursoId, String pcasp, String siopeGrupo, Long contratoId,
            Boolean isPessoal, Boolean isCapital
    ) {
        public static DespesaDto from(Despesa d) {
            return new DespesaDto(d.getId(), d.getCompetencia(), d.getNatureza(), d.getValor(),
                    d.getFonteRecursoId(), d.getPcasp(), d.getSiopeGrupo(), d.getContratoId(),
                    d.isPessoal(), d.isCapital());
        }
    }

    @Schema(name = "DespesaCreateDto")
    public record DespesaCreateDto(
            @NotBlank @Pattern(regexp = "\\d{6}") String competencia,
            @NotBlank String natureza,
            @NotNull @Positive BigDecimal valor,
            Long fonteRecursoId,
            String pcasp,
            String siopeGrupo,
            Long contratoId
    ) {
        public Despesa toEntity() {
            Despesa d = new Despesa();
            d.setCompetencia(competencia);
            d.setNatureza(natureza);
            d.setValor(valor);
            d.setFonteRecursoId(fonteRecursoId);
            d.setPcasp(pcasp);
            d.setSiopeGrupo(siopeGrupo);
            d.setContratoId(contratoId);
            return d;
        }
    }
}
