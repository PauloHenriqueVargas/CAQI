package br.com.caqi.engine.domain;

import br.com.caqi.engine.domain.entity.UsuarioMfa;
import br.com.caqi.engine.domain.repo.UsuarioMfaRepository;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test unitário do MfaService — cobre TOTP + backup codes.
 *
 * Repositório mockado com Map em memória preserva o estado entre operações.
 * Códigos TOTP são calculados via mesma lib do service (DefaultCodeGenerator).
 */
class MfaServiceTest {

    private MfaService service;
    private final Map<String, UsuarioMfa> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        store.clear();
        UsuarioMfaRepository repo = mock(UsuarioMfaRepository.class);
        when(repo.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.<String>getArgument(0))));
        when(repo.save(any(UsuarioMfa.class))).thenAnswer(inv -> {
            UsuarioMfa u = inv.getArgument(0);
            if (u.getCreatedAt() == null) u.setCreatedAt(Instant.now());
            store.put(u.getUsername(), u);
            return u;
        });
        org.mockito.Mockito.doAnswer(inv -> { store.remove(inv.<String>getArgument(0)); return null; })
                .when(repo).deleteById(anyString());

        service = new MfaService(repo, "Município Teste");
    }

    private String totpAtual(String secret) throws Exception {
        return new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6)
                .generate(secret, Instant.now().getEpochSecond() / 30);
    }

    @Test
    @DisplayName("setup() gera secret base32 e otpauth URI com issuer + label corretos")
    void setupGeraSecretEUri() {
        var r = service.setup("admin");

        assertThat(r.secret()).isNotBlank().matches("[A-Z2-7]+");
        assertThat(r.otpauthUri())
                .startsWith("otpauth://totp/")
                .contains("secret=" + r.secret())
                .contains("issuer=CAQi-")
                .contains("admin")
                .contains("algorithm=SHA1")
                .contains("digits=6")
                .contains("period=30");

        assertThat(store.get("admin").getEnabled()).isFalse();
        assertThat(store.get("admin").getBackupCodesHashes()).isEmpty();
    }

    @Test
    @DisplayName("enable() com código correto ativa MFA E retorna 8 backup codes; incorreto falha")
    void enableGeraBackupCodes() throws Exception {
        var setup = service.setup("admin");
        String codigo = totpAtual(setup.secret());

        // Errado
        var falho = service.enable("admin", "000000");
        assertThat(falho.success()).isFalse();
        assertThat(falho.backupCodes()).isEmpty();
        assertThat(store.get("admin").getEnabled()).isFalse();

        // Certo
        var ok = service.enable("admin", codigo);
        assertThat(ok.success()).isTrue();
        assertThat(ok.backupCodes()).hasSize(8);
        assertThat(ok.backupCodes()).allMatch(c -> c.matches("[A-Z2-9]{10}"));
        assertThat(ok.backupCodes()).doesNotHaveDuplicates();

        assertThat(store.get("admin").getEnabled()).isTrue();
        assertThat(store.get("admin").getEnabledAt()).isNotNull();
        assertThat(store.get("admin").getBackupCodesHashes()).hasSize(8);
        assertThat(store.get("admin").getBackupCodesGeneratedAt()).isNotNull();
    }

    @Test
    @DisplayName("verify() aceita TOTP atual quando habilitado")
    void verifyAceitaTotp() throws Exception {
        var setup = service.setup("admin");
        String codigo = totpAtual(setup.secret());

        // Antes do enable, verify devolve false
        assertThat(service.verify("admin", codigo)).isFalse();

        service.enable("admin", codigo);
        assertThat(service.verify("admin", codigo)).isTrue();
    }

    @Test
    @DisplayName("verify() consome backup code (one-time-use) e remove do array")
    void verifyConsomeBackupCode() throws Exception {
        var setup = service.setup("admin");
        var enable = service.enable("admin", totpAtual(setup.secret()));
        List<String> backups = enable.backupCodes();
        String primeiroCode = backups.get(0);

        assertThat(service.countBackupCodesRemaining("admin")).isEqualTo(8);

        // Primeiro uso: ok
        assertThat(service.verify("admin", primeiroCode)).isTrue();
        assertThat(service.countBackupCodesRemaining("admin")).isEqualTo(7);

        // Segundo uso do mesmo code: rejeitado
        assertThat(service.verify("admin", primeiroCode)).isFalse();
        assertThat(service.countBackupCodesRemaining("admin")).isEqualTo(7);

        // Outro backup code ainda funciona
        assertThat(service.verify("admin", backups.get(1))).isTrue();
        assertThat(service.countBackupCodesRemaining("admin")).isEqualTo(6);
    }

    @Test
    @DisplayName("verify() normaliza espaços/hífens e case")
    void verifyNormalizaInput() throws Exception {
        var setup = service.setup("admin");
        var enable = service.enable("admin", totpAtual(setup.secret()));
        String code = enable.backupCodes().get(0);

        // Insere espaço/hífen e usa lowercase — deve aceitar
        String comFormatacao = code.substring(0, 5).toLowerCase() + "-" + code.substring(5).toLowerCase();
        assertThat(service.verify("admin", comFormatacao)).isTrue();
    }

    @Test
    @DisplayName("verify() rejeita formato inválido (não 6 dígitos nem 10 alphanum)")
    void verifyRejeitaFormatoInvalido() throws Exception {
        var setup = service.setup("admin");
        service.enable("admin", totpAtual(setup.secret()));

        assertThat(service.verify("admin", "12345")).isFalse();           // muito curto
        assertThat(service.verify("admin", "ABCDE12345A")).isFalse();     // 11 chars
        assertThat(service.verify("admin", "ABCDE123!@")).isFalse();      // chars inválidos
    }

    @Test
    @DisplayName("regenerateBackupCodes() exige TOTP válido e invalida codes antigos")
    void regenerateInvalidaAntigos() throws Exception {
        var setup = service.setup("admin");
        var enable = service.enable("admin", totpAtual(setup.secret()));
        List<String> antigos = enable.backupCodes();

        // Sem TOTP válido: rejeita
        assertThat(service.regenerateBackupCodes("admin", "000000")).isEmpty();
        assertThat(store.get("admin").getBackupCodesHashes()).hasSize(8); // inalterado

        // Com TOTP válido: novos codes, antigos invalidados
        List<String> novos = service.regenerateBackupCodes("admin", totpAtual(setup.secret()));
        assertThat(novos).hasSize(8);
        assertThat(novos).doesNotContainAnyElementsOf(antigos);

        // Codes antigos não verificam mais
        assertThat(service.verify("admin", antigos.get(0))).isFalse();
        // Novos verificam
        assertThat(service.verify("admin", novos.get(0))).isTrue();
    }

    @Test
    @DisplayName("disable() exige código de confirmação válido")
    void disableExigeCodigo() throws Exception {
        var setup = service.setup("admin");
        String codigo = totpAtual(setup.secret());
        service.enable("admin", codigo);

        assertThat(service.disable("admin", "000000")).isFalse();
        assertThat(store).containsKey("admin");

        assertThat(service.disable("admin", codigo)).isTrue();
        assertThat(store).doesNotContainKey("admin");
    }

    @Test
    @DisplayName("estaHabilitado() + countBackupCodes() refletem o ciclo")
    void estaHabilitadoECiclo() throws Exception {
        assertThat(service.estaHabilitado("admin")).isFalse();
        assertThat(service.countBackupCodesRemaining("admin")).isZero();

        var setup = service.setup("admin");
        assertThat(service.estaHabilitado("admin")).isFalse();
        assertThat(service.countBackupCodesRemaining("admin")).isZero();

        service.enable("admin", totpAtual(setup.secret()));
        assertThat(service.estaHabilitado("admin")).isTrue();
        assertThat(service.countBackupCodesRemaining("admin")).isEqualTo(8);
    }
}
