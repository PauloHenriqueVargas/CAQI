package br.com.caqi.engine.api.dto;

import br.com.caqi.engine.domain.entity.Insumo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public final class InsumoDtos {

    private InsumoDtos() {}

    @Schema(name = "InsumoDto", description = "Insumo do catálogo CAQ")
    public record InsumoDto(
            Long id,
            String codigo,
            String nome,
            String categoria,
            String tipoAplicacao,
            String unidade,
            String etapaAplicavel,
            BigDecimal qtdPadrao
    ) {
        public static InsumoDto from(Insumo i) {
            return new InsumoDto(i.getId(), i.getCodigo(), i.getNome(), i.getCategoria(),
                    i.getTipoAplicacao(), i.getUnidade(), i.getEtapaAplicavel(), i.getQtdPadrao());
        }
    }

    @Schema(name = "InsumoUpsertDto", description = "Payload para criar/atualizar insumo")
    public record InsumoUpsertDto(
            @NotBlank String codigo,
            @NotBlank String nome,
            @NotBlank String categoria,
            @NotBlank @Pattern(regexp = "por_aluno|por_turma|por_escola",
                    message = "tipoAplicacao deve ser por_aluno, por_turma ou por_escola")
            String tipoAplicacao,
            @NotBlank String unidade,
            String etapaAplicavel,
            @NotNull @Positive BigDecimal qtdPadrao
    ) {
        public Insumo toEntity(Insumo target) {
            Insumo i = (target != null) ? target : new Insumo();
            i.setCodigo(codigo);
            i.setNome(nome);
            i.setCategoria(categoria);
            i.setTipoAplicacao(tipoAplicacao);
            i.setUnidade(unidade);
            i.setEtapaAplicavel(etapaAplicavel == null || etapaAplicavel.isBlank() ? "Todas" : etapaAplicavel);
            i.setQtdPadrao(qtdPadrao);
            return i;
        }
    }
}
