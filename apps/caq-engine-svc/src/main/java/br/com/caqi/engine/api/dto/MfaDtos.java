package br.com.caqi.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class MfaDtos {

    private MfaDtos() {}

    @Schema(name = "MfaSetupResponseDto",
            description = "Resposta do POST /auth/mfa/setup. O cliente deve renderizar QR a partir de otpauthUri " +
                    "(ex.: lib qrcode.react no front) e pedir ao usuário o primeiro código para confirmar via /enable.")
    public record MfaSetupResponseDto(
            @Schema(description = "Secret base32 — exibir apenas para fallback de digitação manual")
            String secret,
            @Schema(description = "URI otpauth:// — converter em QR no client")
            String otpauthUri
    ) {
    }

    @Schema(name = "MfaCodigoDto", description = "Código TOTP de 6 dígitos do app autenticador")
    public record MfaCodigoDto(
            @NotBlank @Pattern(regexp = "\\d{6}", message = "Código deve ter 6 dígitos numéricos")
            String codigo
    ) {
    }

    @Schema(name = "MfaStatusDto")
    public record MfaStatusDto(boolean enabled, String username) {}
}
