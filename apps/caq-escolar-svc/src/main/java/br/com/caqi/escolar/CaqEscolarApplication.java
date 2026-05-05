package br.com.caqi.escolar;

import br.com.caqi.escolar.core.TenantProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TenantProperties.class)
public class CaqEscolarApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaqEscolarApplication.class, args);
    }
}
