package br.com.caqi.financeiro.api;

import br.com.caqi.financeiro.api.dto.RetencaoDtos.RequisicaoRetencaoDto;
import br.com.caqi.financeiro.api.dto.RetencaoDtos.ResultadoRetencaoDto;
import br.com.caqi.financeiro.domain.MotorRetencoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tributario")
@RequiredArgsConstructor
@Tag(name = "tributario", description = "Cálculo de retenções tributárias na fonte (IRRF, INSS, ISS, PIS, COFINS, CSLL, DAS)")
public class TributarioController {

    private final MotorRetencoes motor;

    @Operation(summary = "Calcula retenções para um pagamento (stateless — não persiste). " +
            "Trata Simples Nacional (LC 123/2006) com regra simplificada de só ISS na fonte.")
    @PostMapping("/retencoes")
    public ResultadoRetencaoDto calcular(@Valid @RequestBody RequisicaoRetencaoDto requisicao) {
        return motor.calcular(requisicao);
    }
}
