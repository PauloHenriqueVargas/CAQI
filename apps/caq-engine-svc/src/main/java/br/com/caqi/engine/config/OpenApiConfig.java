package br.com.caqi.engine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI caqEngineOpenAPI() {
        final String basicAuth = "basicAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("CAQ Engine — API")
                        .description("""
                                Motor de cálculo CAQ/CAQi: parâmetros, insumos, índices, simulações.

                                **Auth:** HTTP Basic Auth. Usuários (MVP):
                                - `leitor` (LEITOR — GETs)
                                - `gestor` (GESTOR — calcular CAQ, novas vigências de custo)
                                - `admin`  (ADMIN — criar/atualizar insumos e parâmetros)
                                Senhas configuradas via env vars `CAQI_*_PASSWORD`.
                                """)
                        .version("0.2.0")
                        .contact(new Contact().name("CAQI Project").email("contato@caqi.example"))
                        .license(new License().name("Proprietary").url("about:blank")))
                .addSecurityItem(new SecurityRequirement().addList(basicAuth))
                .components(new Components()
                        .addSecuritySchemes(basicAuth, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic Auth — em prod migra para Gov.br OAuth2 (Fase 9)")));
    }
}
