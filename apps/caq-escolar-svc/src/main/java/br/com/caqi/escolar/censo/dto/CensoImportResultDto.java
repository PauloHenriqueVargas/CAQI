package br.com.caqi.escolar.censo.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de uma importação Censo Escolar.
 *
 * Enviado em resposta ao POST /api/v1/escolar/censo/import e
 * /api/v1/escolar/censo/dry-run.
 */
public record CensoImportResultDto(
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
        Integer escolasIgnoradasNaoMunicipal,
        Integer escolasIgnoradasInativas,
        String status,
        String erroMensagem,
        LocalDateTime criadoEm,
        String criadoPor,
        boolean dryRun,
        List<MatriculaResumoDto> resumoMatriculas
) {

    public record MatriculaResumoDto(
            String etapaCodigo,
            Integer qtdAlunos,
            Integer qtdEscolas
    ) {}
}
