package br.com.caqi.engine.api;

import br.com.caqi.engine.api.dto.SimulacaoDtos.CenarioSimulacaoDto;
import br.com.caqi.engine.api.dto.SimulacaoDtos.SimulacaoResultadoDto;
import br.com.caqi.engine.core.exception.NotFoundException;
import br.com.caqi.engine.domain.SimulacaoPreset;
import br.com.caqi.engine.domain.SimulacaoPresets;
import br.com.caqi.engine.domain.SimuladorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "Lista presets de simulação disponíveis (com overrides + base legal)",
            description = "Útil para CACS-Fundeb/CME explorarem cenários típicos sem montar overrides manualmente")
    @GetMapping("/presets")
    public List<SimulacaoPreset> presets() {
        return SimulacaoPresets.TODOS;
    }

    @Operation(summary = "Aplica um preset a um conjunto de escolas/ano",
            description = "Usa as etapasRecomendadas + overrides do preset. Não persiste.")
    @PostMapping("/presets/{nome}")
    public SimulacaoResultadoDto aplicarPreset(
            @PathVariable String nome,
            @Valid @RequestBody PresetExecRequest req
    ) {
        SimulacaoPreset preset = SimulacaoPresets.porNome(nome)
                .orElseThrow(() -> new NotFoundException("SimulacaoPreset", nome));
        var cenario = new CenarioSimulacaoDto(
                req.ano(),
                req.escolas(),
                preset.etapasRecomendadas(),
                preset.alunosPorTurma(),
                preset.qtdPadraoInsumos(),
                preset.custoMultiplierInsumos()
        );
        return simulador.simular(cenario);
    }

    public record PresetExecRequest(
            @NotNull Integer ano,
            @NotEmpty List<String> escolas
    ) {}
}
