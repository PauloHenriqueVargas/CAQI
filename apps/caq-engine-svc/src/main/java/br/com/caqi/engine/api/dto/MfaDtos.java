package br.com.caqi.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

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

    @Schema(name = "MfaCodigoDto",
            description = "Código TOTP (6 dígitos) — usado em /enable, /disable e /regenerate-backup-codes")
    public record MfaCodigoDto(
            @NotBlank @Pattern(regexp = "\\d{6}", message = "Código deve ter 6 dígitos numéricos")
            String codigo
    ) {
    }

    @Schema(name = "MfaVerifyDto",
            description = "Código para /verify — aceita TOTP (6 dígitos) ou backup code (10 alphanum uppercase)")
    public record MfaVerifyDto(
            @NotBlank
            @Pattern(regexp = "\\d{6}|[A-Z2-9]{10}",
                    message = "Código deve ser TOTP (6 dígitos) ou backup code (10 chars alphanum sem 0/O/1/I)")
            String codigo
    ) {
    }

    @Schema(name = "MfaStatusDto")
    public record MfaStatusDto(boolean enabled, String username, int backupCodesRemaining) {}

    @Schema(name = "MfaEnableResponseDto",
            description = "Após enable bem-sucedido, retorna os 8 backup codes UMA ÚNICA VEZ — exibir e exigir que o usuário guarde")
    public record MfaEnableResponseDto(
            boolean enabled,
            String username,
            @Schema(description = "8 backup codes one-time-use; só retornados aqui — não é possível recuperar depois")
            List<String> backupCodes
    ) {
    }

    @Schema(name = "MfaBackupCodesResponseDto", description = "Resposta de /regenerate-backup-codes")
    public record MfaBackupCodesResponseDto(
            List<String> backupCodes
    ) {
    }
}
