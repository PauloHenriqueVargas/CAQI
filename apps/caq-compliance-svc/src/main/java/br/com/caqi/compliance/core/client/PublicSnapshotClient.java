package br.com.caqi.compliance.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP para os endpoints públicos /api/public/transparencia/** dos
 * outros serviços. Sem auth (são publicAll por design).
 *
 * Usado pelo PublicacaoService para fazer snapshots — capturando exatamente
 * o que o cidadão vê, com a mesma serialização Jackson, garantindo que o
 * hash SHA-256 publicado representa o conteúdo público real.
 */
@Component
public class PublicSnapshotClient {

    private final RestClient enginePublic;
    private final RestClient financeiroPublic;

    public PublicSnapshotClient(RestClient enginePublicClient, RestClient financeiroPublicClient) {
        this.enginePublic = enginePublicClient;
        this.financeiroPublic = financeiroPublicClient;
    }

    public JsonNode fundebExecucao(int ano) {
        return financeiroPublic.get()
                .uri(uri -> uri.path("/api/public/transparencia/fundeb-execucao").queryParam("ano", ano).build())
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode siopeQuadro(int ano) {
        return financeiroPublic.get()
                .uri(uri -> uri.path("/api/public/transparencia/siope-quadro").queryParam("ano", ano).build())
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode despesas(int ano) {
        return financeiroPublic.get()
                .uri(uri -> uri.path("/api/public/transparencia/despesas").queryParam("ano", ano).build())
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode contratos() {
        return financeiroPublic.get()
                .uri("/api/public/transparencia/contratos")
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode calculosCaq() {
        return enginePublic.get()
                .uri("/api/public/transparencia/calculos")
                .retrieve()
                .body(JsonNode.class);
    }

    @Configuration
    static class Config {
        @Bean
        RestClient enginePublicClient(@Value("${caqi.clients.engine.base-url}") String baseUrl) {
            return RestClient.builder().baseUrl(baseUrl).build();
        }

        @Bean
        RestClient financeiroPublicClient(@Value("${caqi.clients.financeiro.base-url}") String baseUrl) {
            return RestClient.builder().baseUrl(baseUrl).build();
        }
    }
}
