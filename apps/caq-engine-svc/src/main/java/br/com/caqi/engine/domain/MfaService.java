package br.com.caqi.engine.domain;

import br.com.caqi.engine.domain.entity.UsuarioMfa;
import br.com.caqi.engine.domain.repo.UsuarioMfaRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

/**
 * Provê o ciclo MFA TOTP (RFC 6238) sem alterar o fluxo de login atual:
 *
 *   1. setup(username): gera secret 160-bit Base32, persiste com enabled=false,
 *      devolve URL otpauth:// para o app autenticador (Google Authenticator etc.)
 *      gerar QR no client.
 *   2. enable(username, codigo): valida o primeiro código TOTP — se ok,
 *      marca enabled=true.
 *   3. verify(username, codigo): valida código — usado por integração futura
 *      no fluxo de login (NextAuth segundo passo).
 *
 * Configurações: TOTP de 6 dígitos, janela 30s, algoritmo SHA1
 * (compatibilidade máxima com apps autenticadores).
 */
@Service
@Slf4j
public class MfaService {

    private static final int CODE_DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final int ALLOWED_DISCREPANCY = 1; // janela ±30s

    private final UsuarioMfaRepository repo;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator(20); // 160 bits
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, CODE_DIGITS);
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier verifier;

    private final String issuer;

    public MfaService(UsuarioMfaRepository repo,
                      @Value("${caqi.tenant.municipio-nome:Sistema CAQi}") String municipioNome) {
        this.repo = repo;
        this.issuer = "CAQi-" + municipioNome.replace(' ', '_');

        DefaultCodeVerifier v = new DefaultCodeVerifier(codeGenerator, timeProvider);
        v.setAllowedTimePeriodDiscrepancy(ALLOWED_DISCREPANCY);
        v.setTimePeriod(PERIOD_SECONDS);
        this.verifier = v;
    }

    public record SetupResultado(String secret, String otpauthUri) {}

    @Transactional
    public SetupResultado setup(String username) {
        String secret = secretGenerator.generate();
        UsuarioMfa entity = repo.findById(username).orElseGet(() -> {
            UsuarioMfa novo = new UsuarioMfa();
            novo.setUsername(username);
            return novo;
        });
        entity.setSecretBase32(secret);
        entity.setEnabled(false);
        entity.setEnabledAt(null);
        repo.save(entity);

        String label = URLEncoder.encode(issuer + ":" + username, StandardCharsets.UTF_8);
        String issuerEnc = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String uri = String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                label, secret, issuerEnc, CODE_DIGITS, PERIOD_SECONDS);

        log.info("MFA setup iniciado para {} (issuer={})", username, issuer);
        return new SetupResultado(secret, uri);
    }

    @Transactional
    public boolean enable(String username, String codigo) {
        Optional<UsuarioMfa> opt = repo.findById(username);
        if (opt.isEmpty()) {
            log.warn("MFA enable rejeitado: nenhum setup pendente para {}", username);
            return false;
        }
        UsuarioMfa entity = opt.get();
        if (Boolean.TRUE.equals(entity.getEnabled())) {
            log.info("MFA já estava habilitado para {}", username);
            return true;
        }
        if (!verifier.isValidCode(entity.getSecretBase32(), codigo)) {
            log.warn("MFA enable: código inválido para {}", username);
            return false;
        }
        entity.setEnabled(true);
        entity.setEnabledAt(Instant.now());
        entity.setUltimaValidacao(Instant.now());
        repo.save(entity);
        log.info("MFA habilitado com sucesso para {}", username);
        return true;
    }

    @Transactional
    public boolean verify(String username, String codigo) {
        Optional<UsuarioMfa> opt = repo.findById(username);
        if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getEnabled())) {
            return false;
        }
        UsuarioMfa entity = opt.get();
        if (!verifier.isValidCode(entity.getSecretBase32(), codigo)) {
            log.warn("MFA verify: código inválido para {}", username);
            return false;
        }
        entity.setUltimaValidacao(Instant.now());
        repo.save(entity);
        return true;
    }

    public boolean estaHabilitado(String username) {
        return repo.findById(username)
                .map(u -> Boolean.TRUE.equals(u.getEnabled()))
                .orElse(false);
    }

    @Transactional
    public boolean disable(String username, String codigoConfirmacao) {
        Optional<UsuarioMfa> opt = repo.findById(username);
        if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getEnabled())) {
            return false;
        }
        if (!verifier.isValidCode(opt.get().getSecretBase32(), codigoConfirmacao)) {
            log.warn("MFA disable: código de confirmação inválido para {}", username);
            return false;
        }
        repo.deleteById(username);
        log.info("MFA desabilitado para {}", username);
        return true;
    }
}
