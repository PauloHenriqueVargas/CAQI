package br.com.caqi.compliance.core.client;

import br.com.caqi.compliance.api.dto.ExecucaoFundebDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Cliente HTTP síncrono para o caq-financeiro-svc, com Basic Auth. */
@Component
public class FinanceiroClient {

    private final RestClient client;

    public FinanceiroClient(RestClient financeiroRestClient) {
        this.client = financeiroRestClient;
    }

    public ExecucaoFundebDto execucao(int ano) {
        return client.get()
                .uri(uri -> uri.path("/api/v1/fundeb/execucao").queryParam("ano", ano).build())
                .retrieve()
                .body(ExecucaoFundebDto.class);
    }

    @Configuration
    static class Config {
        @Bean
        RestClient financeiroRestClient(
                @Value("${caqi.clients.financeiro.base-url}") String baseUrl,
                @Value("${caqi.clients.financeiro.username}") String username,
                @Value("${caqi.clients.financeiro.password}") String password
        ) {
            return RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeaders(h -> h.setBasicAuth(username, password))
                    .build();
        }
    }
}
