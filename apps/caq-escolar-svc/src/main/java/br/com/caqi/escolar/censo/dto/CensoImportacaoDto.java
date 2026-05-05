package br.com.caqi.escolar.censo.dto;

import br.com.caqi.escolar.domain.entity.CensoImportacao;

import java.time.LocalDateTime;

public record CensoImportacaoDto(
        Long importacaoId,
        Integer anoCenso,
        String codMunicipioIbge,
        String arquivoNome,
        String arquivoHashSha256,
        Long arquivoTamanhoBytes,
        Integer registrosProcessados,
        Integer registrosMunicipio,
        Integer escolasInseridas,
        Integer escolasAtualizadas,
        Integer matriculasTotal,
        String status,
        String erroMensagem,
        LocalDateTime criadoEm,
        String criadoPor
) {
    public static CensoImportacaoDto from(CensoImportacao e) {
        return new CensoImportacaoDto(
                e.getId(),
                e.getAnoCenso(),
                e.getCodMunicipioIbge(),
                e.getArquivoNome(),
                e.getArquivoHashSha256(),
                e.getArquivoTamanhoBytes(),
                e.getRegistrosProcessados(),
                e.getRegistrosMunicipio(),
                e.getEscolasInseridas(),
                e.getEscolasAtualizadas(),
                e.getMatriculasTotal(),
                e.getStatus().name(),
                e.getErroMensagem(),
                e.getCriadoEm(),
                e.getCriadoPor()
        );
    }
}
