package br.com.caqi.financeiro.api;

import br.com.caqi.financeiro.api.dto.SiopeDtos.SiopeExportDto;
import br.com.caqi.financeiro.api.dto.SiopeDtos.SiopePendenciaDto;
import br.com.caqi.financeiro.domain.SiopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/v1/siope")
@RequiredArgsConstructor
@Tag(name = "siope", description = "Quadro consolidado SIOPE-ready (FNDE) — receitas, despesas, vinculações e pendências")
public class SiopeController {

    private final SiopeService siopeService;

    @Operation(summary = "Gera o quadro consolidado para o exercício (default: ano corrente). " +
            "Use para revisar antes de upload manual no portal SIOPE.")
    @GetMapping("/export")
    public SiopeExportDto export(@RequestParam(required = false) Integer ano) {
        return siopeService.gerar(ano != null ? ano : Year.now().getValue());
    }

    @Operation(summary = "Lista APENAS as pendências detectadas (subset do export — para fluxo de correção)")
    @GetMapping("/status")
    public List<SiopePendenciaDto> status(@RequestParam(required = false) Integer ano) {
        return siopeService.gerar(ano != null ? ano : Year.now().getValue()).pendencias();
    }
}
