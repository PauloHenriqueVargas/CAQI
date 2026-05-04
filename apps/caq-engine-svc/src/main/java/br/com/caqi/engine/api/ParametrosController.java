package br.com.caqi.engine.api;

import br.com.caqi.engine.api.dto.ParametroEtapaDtos.ParametroEtapaDto;
import br.com.caqi.engine.core.exception.NotFoundException;
import br.com.caqi.engine.domain.entity.Etapa;
import br.com.caqi.engine.domain.repo.EtapaRepository;
import br.com.caqi.engine.domain.repo.ParametroEtapaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/caqi/parametros")
@RequiredArgsConstructor
@Tag(name = "caqi-parametros", description = "Parâmetros pedagógicos por etapa/modalidade (alunos/turma, jornada)")
public class ParametrosController {

    private final ParametroEtapaRepository paramRepo;
    private final EtapaRepository etapaRepo;

    @Operation(summary = "Lista todos os parâmetros vigentes na data informada (default: hoje)")
    @GetMapping
    public List<ParametroEtapaDto> listar(@RequestParam(required = false) LocalDate dataReferencia) {
        LocalDate data = (dataReferencia != null) ? dataReferencia : LocalDate.now();
        Map<Long, String> codigoPorEtapaId = etapaRepo.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Etapa::getId, Etapa::getCodigo));

        return etapaRepo.findAll().stream()
                .flatMap(e -> paramRepo.findVigentes(e.getId(), data).stream()
                        .map(p -> ParametroEtapaDto.from(p, codigoPorEtapaId.get(p.getEtapaId()))))
                .toList();
    }

    @Operation(summary = "Lista parâmetros vigentes de uma etapa específica (pode haver múltiplos por 'tempo')")
    @GetMapping("/etapa/{etapaCodigo}")
    public List<ParametroEtapaDto> porEtapa(@PathVariable String etapaCodigo,
                                            @RequestParam(required = false) LocalDate dataReferencia) {
        LocalDate data = (dataReferencia != null) ? dataReferencia : LocalDate.now();
        Etapa etapa = etapaRepo.findByCodigo(etapaCodigo)
                .orElseThrow(() -> new NotFoundException("Etapa", etapaCodigo));
        return paramRepo.findVigentes(etapa.getId(), data).stream()
                .map(p -> ParametroEtapaDto.from(p, etapa.getCodigo()))
                .toList();
    }
}
