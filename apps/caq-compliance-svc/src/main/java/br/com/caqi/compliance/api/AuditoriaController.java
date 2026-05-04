package br.com.caqi.compliance.api;

import br.com.caqi.compliance.api.dto.LogAuditoriaDto;
import br.com.caqi.compliance.domain.AuditChainService;
import br.com.caqi.compliance.domain.repo.LogAuditoriaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/compliance/auditoria")
@RequiredArgsConstructor
@Tag(name = "compliance-auditoria", description = "Cadeia imutável de logs de auditoria com chain SHA-256")
public class AuditoriaController {

    private final LogAuditoriaRepository repo;
    private final AuditChainService chainService;

    @Operation(summary = "Lista logs em ordem cronológica")
    @GetMapping
    public List<LogAuditoriaDto> listar() {
        return repo.findAllByOrderByIdAsc().stream().map(LogAuditoriaDto::from).toList();
    }

    @Operation(summary = "Verifica integridade da cadeia (recalcula todos os hashes). " +
            "Retorna {integro:true} ou {integro:false, primeiroLogQuebrado: id}.")
    @GetMapping("/verificar")
    public Map<String, Object> verificar() {
        Optional<Long> primeiroQuebrado = chainService.verificarIntegridade();
        if (primeiroQuebrado.isEmpty()) {
            return Map.of("integro", true);
        }
        return Map.of("integro", false, "primeiroLogQuebrado", primeiroQuebrado.get());
    }
}
