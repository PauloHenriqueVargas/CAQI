package br.com.caqi.engine.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Senhas dos 3 usuários de aplicação para o MVP.
 * Em produção isso é substituído por Gov.br OAuth2 (Fase 9) e usuários
 * em DB; o user store em memória atende apenas ao MVP / piloto interno.
 */
@ConfigurationProperties(prefix = "caqi.security.users")
public record SecurityProperties(
        UserCreds leitor,
        UserCreds gestor,
        UserCreds admin
) {
    public record UserCreds(String username, String password) {}
}
