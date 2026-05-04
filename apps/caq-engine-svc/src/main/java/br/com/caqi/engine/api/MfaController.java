package br.com.caqi.engine.api;

import br.com.caqi.engine.api.dto.MfaDtos.MfaBackupCodesResponseDto;
import br.com.caqi.engine.api.dto.MfaDtos.MfaCodigoDto;
import br.com.caqi.engine.api.dto.MfaDtos.MfaEnableResponseDto;
import br.com.caqi.engine.api.dto.MfaDtos.MfaSetupResponseDto;
import br.com.caqi.engine.api.dto.MfaDtos.MfaStatusDto;
import br.com.caqi.engine.api.dto.MfaDtos.MfaVerifyDto;
import br.com.caqi.engine.domain.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "auth-mfa", description = "Multi-Factor Authentication via TOTP (RFC 6238) com backup codes para recuperação.")
public class MfaController {

    private final MfaService mfaService;

    @Operation(summary = "Inicia o setup do MFA — gera secret e devolve URI otpauth para QR.")
    @PostMapping("/setup")
    public MfaSetupResponseDto setup() {
        var r = mfaService.setup(currentUsername());
        return new MfaSetupResponseDto(r.secret(), r.otpauthUri());
    }

    @Operation(summary = "Habilita MFA confirmando o primeiro código TOTP. " +
            "Em sucesso, RETORNA 8 BACKUP CODES UMA ÚNICA VEZ — exibir e exigir que o usuário guarde.")
    @PostMapping("/enable")
    public ResponseEntity<MfaEnableResponseDto> enable(@Valid @RequestBody MfaCodigoDto req) {
        String username = currentUsername();
        var r = mfaService.enable(username, req.codigo());
        if (!r.success()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MfaEnableResponseDto(false, username, List.of()));
        }
        return ResponseEntity.ok(new MfaEnableResponseDto(true, username, r.backupCodes()));
    }

    @Operation(summary = "Valida código TOTP (6 dígitos) OU backup code (10 alphanum). " +
            "Backup codes são one-time-use (consumidos no sucesso).")
    @PostMapping("/verify")
    public ResponseEntity<MfaStatusDto> verify(@Valid @RequestBody MfaVerifyDto req) {
        String username = currentUsername();
        boolean ok = mfaService.verify(username, req.codigo());
        return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                .body(new MfaStatusDto(ok && mfaService.estaHabilitado(username), username,
                        mfaService.countBackupCodesRemaining(username)));
    }

    @Operation(summary = "Status do MFA do usuário corrente, incluindo número de backup codes restantes")
    @GetMapping("/status")
    public MfaStatusDto status() {
        String username = currentUsername();
        boolean enabled = mfaService.estaHabilitado(username);
        int remaining = enabled ? mfaService.countBackupCodesRemaining(username) : 0;
        return new MfaStatusDto(enabled, username, remaining);
    }

    @Operation(summary = "Desabilita MFA — exige código TOTP atual como confirmação")
    @PostMapping("/disable")
    public ResponseEntity<MfaStatusDto> disable(@Valid @RequestBody MfaCodigoDto req) {
        String username = currentUsername();
        boolean ok = mfaService.disable(username, req.codigo());
        return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new MfaStatusDto(!ok && mfaService.estaHabilitado(username), username, 0));
    }

    @Operation(summary = "Regenera os 8 backup codes — invalida o conjunto antigo. Exige TOTP válido.")
    @PostMapping("/regenerate-backup-codes")
    public ResponseEntity<MfaBackupCodesResponseDto> regenerateBackupCodes(@Valid @RequestBody MfaCodigoDto req) {
        List<String> codes = mfaService.regenerateBackupCodes(currentUsername(), req.codigo());
        if (codes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MfaBackupCodesResponseDto(List.of()));
        }
        return ResponseEntity.ok(new MfaBackupCodesResponseDto(codes));
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Sessão não autenticada — MfaController exige Basic Auth válido");
        }
        return auth.getName();
    }
}
