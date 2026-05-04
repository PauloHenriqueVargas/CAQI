package br.com.caqi.compliance.api;

import br.com.caqi.compliance.api.dto.NotificacaoDto;
import br.com.caqi.compliance.domain.AvaliadorComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Tag(name = "compliance", description = "Avaliação de regras legais (MDE 25%, Fundeb 70%, VAAT 15%, VAAR)")
public class AvaliacaoController {

    private final AvaliadorComplianceService avaliador;

    @Operation(summary = "Executa a avaliação para o ano (default: ano corrente). Cria notificações para cada regra violada.")
    @PostMapping("/avaliar")
    public List<NotificacaoDto> avaliar(@RequestParam(required = false) Integer ano) {
        int alvo = (ano != null) ? ano : Year.now().getValue();
        return avaliador.avaliarAno(alvo).stream().map(NotificacaoDto::from).toList();
    }
}
