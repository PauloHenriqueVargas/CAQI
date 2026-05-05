package br.com.caqi.escolar.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Identificação do tenant (1 instância CAQi por município).
 *
 * O código IBGE 7-dígitos é usado para filtrar microdados Censo INEP por
 * CO_MUNICIPIO, evitando importar escolas de outros municípios contidas
 * no mesmo arquivo.
 */
@ConfigurationProperties(prefix = "caqi.tenant")
public record TenantProperties(
        String municipioId,
        String municipioNome,
        String codMunicipioIbge
) {}
