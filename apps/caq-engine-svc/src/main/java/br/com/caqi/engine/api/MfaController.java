package br.com.caqi.engine.api;

import br.com.caqi.engine.api.dto.MfaDtos.MfaCodigoDto;
import br.com.caqi.engine.api.dto.MfaDtos.MfaSetupResponseDto;
import br.com.caqi.engine.api.dto.MfaDtos.MfaStatusDto;
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

@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "auth-mfa", description = "Multi-Factor Authentication via TOTP (RFC 6238). Usuário autenticado via Basic Auth gerencia o próprio MFA.")
public class MfaController {

    private final MfaService mfaService;

    @Operation(summary = "Inicia o setup do MFA — gera secret e devolve URI otpauth para QR. " +
            "Setup pendente NÃO bloqueia login até /enable confirmar o primeiro código.")
    @PostMapping("/setup")
    public MfaSetupResponseDto setup() {
        String username = currentUsername();
        var r = mfaService.setup(username);
        return new MfaSetupResponseDto(r.secret(), r.otpauthUri());
    }

    @Operation(summary = "Habilita MFA confirmando o primeiro código TOTP. 200 = ok; 400 = código inválido.")
    @PostMapping("/enable")
    public ResponseEntity<MfaStatusDto> enable(@Valid @RequestBody MfaCodigoDto req) {
        String username = currentUsername();
        boolean ok = mfaService.enable(username, req.codigo());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MfaStatusDto(false, username));
        }
        return ResponseEntity.ok(new MfaStatusDto(true, username));
    }

    @Operation(summary = "Valida código TOTP atual. Usado por integração futura no fluxo de login (segundo fator).")
    @PostMapping("/verify")
    public ResponseEntity<MfaStatusDto> verify(@Valid @RequestBody MfaCodigoDto req) {
        String username = currentUsername();
        boolean ok = mfaService.verify(username, req.codigo());
        return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                .body(new MfaStatusDto(ok, username));
    }

    @Operation(summary = "Status do MFA do usuário corrente")
    @GetMapping("/status")
    public MfaStatusDto status() {
        String username = currentUsername();
        return new MfaStatusDto(mfaService.estaHabilitado(username), username);
    }

    @Operation(summary = "Desabilita MFA — exige código TOTP atual como confirmação")
    @PostMapping("/disable")
    public ResponseEntity<MfaStatusDto> disable(@Valid @RequestBody MfaCodigoDto req) {
        String username = currentUsername();
        boolean ok = mfaService.disable(username, req.codigo());
        return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new MfaStatusDto(!ok && mfaService.estaHabilitado(username), username));
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Sessão não autenticada — MfaController exige Basic Auth válido");
        }
        return auth.getName();
    }
}
