package br.com.caqi.escolar.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Value("${spring.application.name}")
    private String serviceName;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", serviceName, "timestamp", Instant.now().toString());
    }
}
