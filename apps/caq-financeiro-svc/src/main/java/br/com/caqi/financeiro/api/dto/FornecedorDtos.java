package br.com.caqi.financeiro.api.dto;

import br.com.caqi.financeiro.domain.entity.Fornecedor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class FornecedorDtos {

    private FornecedorDtos() {}

    @Schema(name = "FornecedorDto")
    public record FornecedorDto(Long id, String cnpj, String nome, Boolean optanteSimples, String municipio) {
        public static FornecedorDto from(Fornecedor f) {
            return new FornecedorDto(f.getId(), f.getCnpj(), f.getNome(), f.getOptanteSimples(), f.getMunicipio());
        }
    }

    @Schema(name = "FornecedorUpsertDto")
    public record FornecedorUpsertDto(
            @NotBlank String cnpj,
            @NotBlank String nome,
            @NotNull Boolean optanteSimples,
            String municipio
    ) {
        public Fornecedor toEntity(Fornecedor target) {
            Fornecedor f = (target != null) ? target : new Fornecedor();
            f.setCnpj(cnpj);
            f.setNome(nome);
            f.setOptanteSimples(optanteSimples);
            f.setMunicipio(municipio);
            return f;
        }
    }
}
