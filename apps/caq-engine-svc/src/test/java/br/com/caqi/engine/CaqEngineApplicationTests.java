package br.com.caqi.engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class CaqEngineApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("caqi_test")
            .withUsername("caqi")
            .withPassword("caqi_test_password");

    @DynamicPropertySource
    static void disableRabbit(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
    }

    @Test
    void contextLoads() {
        // Smoke test: garante que o Spring sobe com Postgres real (Testcontainers)
        // e Flyway aplica V0001__initial_schema.sql sem erro.
    }
}
