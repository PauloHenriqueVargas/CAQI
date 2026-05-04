package br.com.caqi.engine.api;

import br.com.caqi.engine.api.dto.RequisicaoCalculoDto;
import br.com.caqi.engine.api.dto.ResultadoCalculoDto;
import br.com.caqi.engine.service.CaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/caqi")
@RequiredArgsConstructor
@Tag(name = "caqi", description = "Cálculo CAQ/CAQi — motor por etapa/escola/ano")
public class CaqController {

    private final CaqService caqService;

    @Operation(
            summary = "Executa o cálculo CAQi/CAQ por etapa/escola para um ano",
            description = "Aplica matriz Insumo→Custo anualizada, valida regras Fundeb (70%/15%) e MDE (25%) "
                    + "e devolve R$/aluno/ano + memória de cálculo. NÃO IMPLEMENTADO — Fase 2 do roadmap."
    )
    @PostMapping("/calculos")
    public ResponseEntity<ResultadoCalculoDto> calcular(@Valid @RequestBody RequisicaoCalculoDto requisicao) {
        return ResponseEntity.ok(caqService.calcular(requisicao));
    }
}
