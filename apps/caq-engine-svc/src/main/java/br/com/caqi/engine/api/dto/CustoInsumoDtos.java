package br.com.caqi.engine.api.dto;

import br.com.caqi.engine.domain.entity.CustoInsumo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CustoInsumoDtos {

    private CustoInsumoDtos() {}

    @Schema(name = "CustoInsumoDto", description = "Custo vigente de um insumo num perfil")
    public record CustoInsumoDto(
            Long id,
            Long insumoId,
            BigDecimal custoUnitario,
            String fontePreco,
            String indiceAtualizacao,
            String perfil,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim
    ) {
        public static CustoInsumoDto from(CustoInsumo c) {
            return new CustoInsumoDto(c.getId(), c.getInsumoId(), c.getCustoUnitario(),
                    c.getFontePreco(), c.getIndiceAtualizacao(), c.getPerfil(),
                    c.getVigenciaInicio(), c.getVigenciaFim());
        }
    }

    @Schema(name = "CustoInsumoCreateDto", description = "Cria nova vigência de custo para um insumo")
    public record CustoInsumoCreateDto(
            @NotNull @Positive BigDecimal custoUnitario,
            String fontePreco,
            String indiceAtualizacao,
            @NotNull @Pattern(regexp = "minimo|adequado", message = "perfil deve ser 'minimo' ou 'adequado'")
            String perfil,
            @NotNull LocalDate vigenciaInicio,
            LocalDate vigenciaFim
    ) {
        public CustoInsumo toEntity(Long insumoId) {
            CustoInsumo c = new CustoInsumo();
            c.setInsumoId(insumoId);
            c.setCustoUnitario(custoUnitario);
            c.setFontePreco(fontePreco);
            c.setIndiceAtualizacao(indiceAtualizacao);
            c.setPerfil(perfil);
            c.setVigenciaInicio(vigenciaInicio);
            c.setVigenciaFim(vigenciaFim);
            return c;
        }
    }
}
