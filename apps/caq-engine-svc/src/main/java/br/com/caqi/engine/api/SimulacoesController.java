package br.com.caqi.engine.api;

import br.com.caqi.engine.api.dto.SimulacaoDtos.CenarioSimulacaoDto;
import br.com.caqi.engine.api.dto.SimulacaoDtos.SimulacaoResultadoDto;
import br.com.caqi.engine.domain.SimuladorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/caqi/simulacoes")
@RequiredArgsConstructor
@Tag(name = "caqi-simulacoes",
        description = "Simulador 'e se?' — recalcula CAQ/CAQi com overrides hipotéticos e devolve o impacto")
public class SimulacoesController {

    private final SimuladorService simulador;

    @Operation(summary = "Executa simulação. Não persiste nada — devolve atual vs simulado + diferenças")
    @PostMapping
    public SimulacaoResultadoDto simular(@Valid @RequestBody CenarioSimulacaoDto cenario) {
        return simulador.simular(cenario);
    }
}
