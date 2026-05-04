package br.com.caqi.financeiro.api.dto;

import br.com.caqi.financeiro.domain.entity.Receita;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public final class ReceitaDtos {

    private ReceitaDtos() {}

    @Schema(name = "ReceitaDto")
    public record ReceitaDto(Long id, String competencia, BigDecimal valor, String origem, String pcasp) {
        public static ReceitaDto from(Receita r) {
            return new ReceitaDto(r.getId(), r.getCompetencia(), r.getValor(), r.getOrigem(), r.getPcasp());
        }
    }

    @Schema(name = "ReceitaCreateDto")
    public record ReceitaCreateDto(
            @NotBlank @Pattern(regexp = "\\d{6}", message = "competencia deve ser YYYYMM (6 dígitos)")
            String competencia,
            @NotNull @Positive BigDecimal valor,
            @NotBlank String origem,
            String pcasp
    ) {
        public Receita toEntity() {
            Receita r = new Receita();
            r.setCompetencia(competencia);
            r.setValor(valor);
            r.setOrigem(origem);
            r.setPcasp(pcasp);
            return r;
        }
    }
}
