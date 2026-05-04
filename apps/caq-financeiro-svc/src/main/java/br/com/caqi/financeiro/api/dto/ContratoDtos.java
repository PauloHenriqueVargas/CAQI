package br.com.caqi.financeiro.api.dto;

import br.com.caqi.financeiro.domain.entity.Contrato;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ContratoDtos {

    private ContratoDtos() {}

    @Schema(name = "ContratoDto")
    public record ContratoDto(
            Long id, Long fornecedorId, String objeto, LocalDate dataAssinatura,
            BigDecimal valorGlobal, String modalidade, String pncpId
    ) {
        public static ContratoDto from(Contrato c) {
            return new ContratoDto(c.getId(), c.getFornecedorId(), c.getObjeto(), c.getDataAssinatura(),
                    c.getValorGlobal(), c.getModalidade(), c.getPncpId());
        }
    }

    @Schema(name = "ContratoCreateDto")
    public record ContratoCreateDto(
            @NotNull Long fornecedorId,
            @NotBlank String objeto,
            @NotNull LocalDate dataAssinatura,
            @Positive BigDecimal valorGlobal,
            @NotBlank @Pattern(regexp = "PREGAO_ELETRONICO|CONCORRENCIA|DISPENSA|INEXIGIBILIDADE|DIALOGO_COMPETITIVO|CONCURSO|LEILAO",
                    message = "modalidade deve ser uma da Lei 14.133/2021")
            String modalidade,
            String pncpId
    ) {
        public Contrato toEntity() {
            Contrato c = new Contrato();
            c.setFornecedorId(fornecedorId);
            c.setObjeto(objeto);
            c.setDataAssinatura(dataAssinatura);
            c.setValorGlobal(valorGlobal);
            c.setModalidade(modalidade);
            c.setPncpId(pncpId);
            return c;
        }
    }
}
