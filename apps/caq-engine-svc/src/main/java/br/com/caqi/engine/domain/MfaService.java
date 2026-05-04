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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Provê o ciclo MFA TOTP (RFC 6238) + backup codes para recuperação:
 *
 *   - setup(username): gera secret 160-bit Base32 + URI otpauth.
 *   - enable(username, codigo): valida o primeiro código TOTP, ativa MFA
 *     e GERA 8 backup codes one-time-use (retornados UMA vez, hashed em DB).
 *   - verify(username, codigo): aceita TOTP (6 dígitos) OU backup code
 *     (10 alphanum). Backup code é consumido (removido do array de hashes).
 *   - regenerateBackupCodes(username, codigoTotp): exige TOTP válido,
 *     invalida codes antigos e gera 8 novos.
 *   - disable(username, codigo): remove o registro inteiro.
 *
 * Configurações TOTP: 6 dígitos, janela 30s, ±1 (tolerância 60s),
 * algoritmo SHA1 (compat máxima com Google Authenticator/Aegis/etc.).
 *
 * Backup codes: 10 chars alphanum uppercase, alfabeto sem ambíguos
 * (sem 0/O/1/I), entropia ≈ 50 bits cada. Hash SHA-256 hex sem salt
 * (entropia alta dispensa salt/iteration).
 */
@Service
@Slf4j
public class MfaService {

    private static final int CODE_DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final int ALLOWED_DISCREPANCY = 1;

    /** Alfabeto sem caracteres ambíguos (0/O/1/I) para reduzir erros de digitação. */
    private static final String BACKUP_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int BACKUP_CODE_LENGTH = 10;
    private static final int BACKUP_CODE_COUNT = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioMfaRepository repo;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator(20);
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

    public record EnableResultado(boolean success, List<String> backupCodes) {}

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
        entity.setBackupCodesHashes(new ArrayList<>());
        entity.setBackupCodesGeneratedAt(null);
        repo.save(entity);

        String label = URLEncoder.encode(issuer + ":" + username, StandardCharsets.UTF_8);
        String issuerEnc = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String uri = String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                label, secret, issuerEnc, CODE_DIGITS, PERIOD_SECONDS);

        log.info("MFA setup iniciado para {} (issuer={})", username, issuer);
        return new SetupResultado(secret, uri);
    }

    /**
     * Ativa MFA validando o primeiro código TOTP. Em sucesso, gera e retorna
     * 8 backup codes raw — o cliente DEVE exibir UMA ÚNICA VEZ ao usuário.
     */
    @Transactional
    public EnableResultado enable(String username, String codigo) {
        Optional<UsuarioMfa> opt = repo.findById(username);
        if (opt.isEmpty()) {
            log.warn("MFA enable rejeitado: nenhum setup pendente para {}", username);
            return new EnableResultado(false, List.of());
        }
        UsuarioMfa entity = opt.get();
        if (Boolean.TRUE.equals(entity.getEnabled())) {
            log.info("MFA já estava habilitado para {} — re-enable não regera backup codes", username);
            return new EnableResultado(true, List.of());
        }
        if (!verifier.isValidCode(entity.getSecretBase32(), codigo)) {
            log.warn("MFA enable: código TOTP inválido para {}", username);
            return new EnableResultado(false, List.of());
        }

        List<String> rawCodes = generateBackupCodes();
        entity.setEnabled(true);
        entity.setEnabledAt(Instant.now());
        entity.setUltimaValidacao(Instant.now());
        entity.setBackupCodesHashes(rawCodes.stream().map(MfaService::hashCode).toList());
        entity.setBackupCodesGeneratedAt(Instant.now());
        repo.save(entity);
        log.info("MFA habilitado para {} — {} backup codes gerados", username, rawCodes.size());
        return new EnableResultado(true, rawCodes);
    }

    /**
     * Aceita TOTP (6 dígitos) OU backup code (10 alphanum uppercase).
     * Backup code é one-time — removido do array após uso.
     */
    @Transactional
    public boolean verify(String username, String codigo) {
        Optional<UsuarioMfa> opt = repo.findById(username);
        if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getEnabled())) return false;
        UsuarioMfa entity = opt.get();

        String norm = codigo == null ? "" : codigo.replace(" ", "").replace("-", "").toUpperCase();

        if (norm.matches("\\d{6}")) {
            if (verifier.isValidCode(entity.getSecretBase32(), norm)) {
                entity.setUltimaValidacao(Instant.now());
                repo.save(entity);
                return true;
            }
            log.warn("MFA verify: código TOTP inválido para {}", username);
            return false;
        }

        if (norm.matches("[A-Z2-9]{" + BACKUP_CODE_LENGTH + "}")) {
            String hash = hashCode(norm);
            List<String> hashes = entity.getBackupCodesHashes();
            if (hashes != null && hashes.contains(hash)) {
                List<String> remaining = new ArrayList<>(hashes);
                remaining.remove(hash);
                entity.setBackupCodesHashes(remaining);
                entity.setUltimaValidacao(Instant.now());
                repo.save(entity);
                log.info("MFA: backup code consumido para {} — restam {}", username, remaining.size());
                return true;
            }
            log.warn("MFA verify: backup code inválido para {}", username);
            return false;
        }

        log.warn("MFA verify: formato de código inválido para {} (esperado 6 dígitos ou {} alphanum)",
                username, BACKUP_CODE_LENGTH);
        return false;
    }

    public boolean estaHabilitado(String username) {
        return repo.findById(username)
                .map(u -> Boolean.TRUE.equals(u.getEnabled()))
                .orElse(false);
    }

    public int countBackupCodesRemaining(String username) {
        return repo.findById(username)
                .map(u -> u.getBackupCodesHashes() == null ? 0 : u.getBackupCodesHashes().size())
                .orElse(0);
    }

    /**
     * Invalida o conjunto atual e gera novos backup codes — exige TOTP válido
     * para evitar abuso quando uma sessão é comprometida.
     */
    @Transactional
    public List<String> regenerateBackupCodes(String username, String codigoTotp) {
        Optional<UsuarioMfa> opt = repo.findById(username);
        if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getEnabled())) return List.of();
        UsuarioMfa entity = opt.get();
        if (!verifier.isValidCode(entity.getSecretBase32(), codigoTotp)) {
            log.warn("MFA regenerate: TOTP inválido para {}", username);
            return List.of();
        }

        List<String> rawCodes = generateBackupCodes();
        entity.setBackupCodesHashes(rawCodes.stream().map(MfaService::hashCode).toList());
        entity.setBackupCodesGeneratedAt(Instant.now());
        repo.save(entity);
        log.info("MFA: backup codes regenerados para {} ({} novos)", username, rawCodes.size());
        return rawCodes;
    }

    @Transactional
    public boolean disable(String username, String codigoConfirmacao) {
        Optional<UsuarioMfa> opt = repo.findById(username);
        if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getEnabled())) return false;
        if (!verifier.isValidCode(opt.get().getSecretBase32(), codigoConfirmacao)) {
            log.warn("MFA disable: código de confirmação inválido para {}", username);
            return false;
        }
        repo.deleteById(username);
        log.info("MFA desabilitado para {}", username);
        return true;
    }

    // ─────────────── helpers ───────────────

    private static List<String> generateBackupCodes() {
        return IntStream.range(0, BACKUP_CODE_COUNT)
                .mapToObj(i -> generateOneCode())
                .toList();
    }

    private static String generateOneCode() {
        StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            sb.append(BACKUP_CODE_ALPHABET.charAt(RANDOM.nextInt(BACKUP_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    static String hashCode(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
