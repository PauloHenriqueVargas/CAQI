package br.com.caqi.financeiro.api.dto;

import br.com.caqi.financeiro.domain.entity.FonteRecurso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class FonteRecursoDtos {

    private FonteRecursoDtos() {}

    @Schema(name = "FonteRecursoDto")
    public record FonteRecursoDto(Long id, String tipo, String descricao) {
        public static FonteRecursoDto from(FonteRecurso f) {
            return new FonteRecursoDto(f.getId(), f.getTipo(), f.getDescricao());
        }
    }

    @Schema(name = "FonteRecursoCreateDto")
    public record FonteRecursoCreateDto(
            @NotBlank @Pattern(regexp = "Propria|VAAF|VAAT|VAAR|Outras") String tipo,
            String descricao
    ) {
        public FonteRecurso toEntity() {
            FonteRecurso f = new FonteRecurso();
            f.setTipo(tipo);
            f.setDescricao(descricao);
            return f;
        }
    }
}
