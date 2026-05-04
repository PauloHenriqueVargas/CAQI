package br.com.caqi.engine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI caqEngineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CAQ Engine — API")
                        .description("Motor de cálculo CAQ/CAQi: parâmetros, insumos, índices, simulações.")
                        .version("0.1.0")
                        .contact(new Contact().name("CAQI Project").email("contato@caqi.example"))
                        .license(new License().name("Proprietary").url("about:blank")));
    }
}
