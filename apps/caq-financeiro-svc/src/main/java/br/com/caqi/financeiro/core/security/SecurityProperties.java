package br.com.caqi.financeiro.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Mesmo modelo do caq-engine-svc — em uma instância municipal espera-se config compartilhada. */
@ConfigurationProperties(prefix = "caqi.security.users")
public record SecurityProperties(
        UserCreds leitor,
        UserCreds gestor,
        UserCreds admin
) {
    public record UserCreds(String username, String password) {}
}
