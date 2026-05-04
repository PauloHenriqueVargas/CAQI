package br.com.caqi.financeiro.api;

import br.com.caqi.financeiro.api.dto.ExecucaoFundebDto;
import br.com.caqi.financeiro.domain.FundebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;

@RestController
@RequestMapping("/api/v1/fundeb")
@RequiredArgsConstructor
@Tag(name = "fundeb", description = "Execução das vinculações constitucionais e legais (MDE 25%, Fundeb 70%, VAAT 15%)")
public class FundebController {

    private final FundebService fundebService;

    @Operation(summary = "Calcula a execução das vinculações para o ano informado (default: ano corrente)")
    @GetMapping("/execucao")
    public ExecucaoFundebDto execucao(@RequestParam(required = false) Integer ano) {
        return fundebService.calcular(ano != null ? ano : Year.now().getValue());
    }
}
