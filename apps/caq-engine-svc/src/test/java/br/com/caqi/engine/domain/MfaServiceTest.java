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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test unitário do MfaService — valida o ciclo setup → enable → verify
 * usando o gerador de código TOTP da própria lib para simular o app
 * autenticador no momento atual.
 *
 * NOTA: o repositório é mockado com um Map em memória para preservar
 * o estado entre operações (setup persiste, enable lê e atualiza).
 */
class MfaServiceTest {

    private MfaService service;
    private UsuarioMfaRepository repo;
    private final Map<String, UsuarioMfa> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        store.clear();
        repo = mock(UsuarioMfaRepository.class);
        when(repo.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.<String>getArgument(0))));
        when(repo.save(any(UsuarioMfa.class))).thenAnswer(inv -> {
            UsuarioMfa u = inv.getArgument(0);
            if (u.getCreatedAt() == null) u.setCreatedAt(Instant.now());
            store.put(u.getUsername(), u);
            return u;
        });

        service = new MfaService(repo, "Município Teste");
    }

    @Test
    @DisplayName("setup() gera secret base32 e otpauth URI com issuer + label corretos")
    void setupGeraSecretEUri() {
        var r = service.setup("admin");

        assertThat(r.secret()).isNotBlank().matches("[A-Z2-7]+"); // base32
        assertThat(r.otpauthUri())
                .startsWith("otpauth://totp/")
                .contains("secret=" + r.secret())
                .contains("issuer=CAQi-")
                .contains("admin")
                .contains("algorithm=SHA1")
                .contains("digits=6")
                .contains("period=30");

        assertThat(store.get("admin").getEnabled()).isFalse();
    }

    @Test
    @DisplayName("enable() com código correto ativa MFA; código incorreto falha")
    void enableValidaCodigoCorreto() throws Exception {
        var setup = service.setup("admin");

        // Calcula código atual usando a mesma lib que o service usa
        String codigoCerto = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6)
                .generate(setup.secret(), Instant.now().getEpochSecond() / 30);

        assertThat(service.enable("admin", "000000")).isFalse();   // código errado
        assertThat(store.get("admin").getEnabled()).isFalse();

        assertThat(service.enable("admin", codigoCerto)).isTrue();  // código correto
        assertThat(store.get("admin").getEnabled()).isTrue();
        assertThat(store.get("admin").getEnabledAt()).isNotNull();
    }

    @Test
    @DisplayName("verify() só valida quando MFA já habilitado")
    void verifySoFuncionaQuandoEnabled() throws Exception {
        var setup = service.setup("admin");
        String codigo = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6)
                .generate(setup.secret(), Instant.now().getEpochSecond() / 30);

        // Antes do enable, verify devolve false (MFA não está ativo)
        assertThat(service.verify("admin", codigo)).isFalse();

        service.enable("admin", codigo);
        assertThat(service.verify("admin", codigo)).isTrue();
    }

    @Test
    @DisplayName("disable() exige código de confirmação válido")
    void disableExigeCodigo() throws Exception {
        var setup = service.setup("admin");
        String codigo = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6)
                .generate(setup.secret(), Instant.now().getEpochSecond() / 30);
        service.enable("admin", codigo);

        assertThat(service.disable("admin", "000000")).isFalse();
        assertThat(store).containsKey("admin");

        assertThat(service.disable("admin", codigo)).isTrue();
        assertThat(store).doesNotContainKey("admin");
    }

    @Test
    @DisplayName("estaHabilitado() reflete o ciclo de vida")
    void estaHabilitadoCiclo() throws Exception {
        assertThat(service.estaHabilitado("admin")).isFalse(); // sem setup

        var setup = service.setup("admin");
        assertThat(service.estaHabilitado("admin")).isFalse(); // setup mas não enable

        String codigo = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6)
                .generate(setup.secret(), Instant.now().getEpochSecond() / 30);
        service.enable("admin", codigo);
        assertThat(service.estaHabilitado("admin")).isTrue();
    }
}
