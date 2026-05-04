package br.com.caqi.engine.api.dto;

import br.com.caqi.engine.domain.entity.ParametroEtapa;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ParametroEtapaDtos {

    private ParametroEtapaDtos() {}

    @Schema(name = "ParametroEtapaDto", description = "Parâmetros vigentes por etapa/tempo")
    public record ParametroEtapaDto(
            Long id,
            Long etapaId,
            String etapaCodigo,
            String tempo,
            BigDecimal alunosPorTurma,
            BigDecimal cargaHorariaDocenteHSem,
            BigDecimal jornadaDiasAno,
            BigDecimal coefRural,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim
    ) {
        public static ParametroEtapaDto from(ParametroEtapa p, String etapaCodigo) {
            return new ParametroEtapaDto(p.getId(), p.getEtapaId(), etapaCodigo, p.getTempo(),
                    p.getAlunosPorTurma(), p.getCargaHorariaDocenteHSem(), p.getJornadaDiasAno(),
                    p.getCoefRural(), p.getVigenciaInicio(), p.getVigenciaFim());
        }
    }
}
