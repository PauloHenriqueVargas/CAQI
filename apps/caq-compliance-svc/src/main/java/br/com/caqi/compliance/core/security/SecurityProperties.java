package br.com.caqi.compliance.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caqi.security.users")
public record SecurityProperties(
        UserCreds leitor,
        UserCreds gestor,
        UserCreds admin
) {
    public record UserCreds(String username, String password) {}
}
