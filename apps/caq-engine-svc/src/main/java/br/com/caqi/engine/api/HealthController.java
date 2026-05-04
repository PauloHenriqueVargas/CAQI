package br.com.caqi.engine.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "health", description = "Liveness e metadados do serviço")
public class HealthController {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${caqi.tenant.municipio-id:000000}")
    private String municipioId;

    @Value("${caqi.tenant.municipio-nome:Municipio Exemplo}")
    private String municipioNome;

    @Operation(summary = "Health check liviano (não toca DB) — para liveness probes do k8s")
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", serviceName,
                "tenant", Map.of(
                        "municipio_id", municipioId,
                        "municipio_nome", municipioNome
                ),
                "timestamp", Instant.now().toString()
        );
    }
}
