package br.com.caqi.compliance.domain;

import br.com.caqi.compliance.core.client.PublicSnapshotClient;
import br.com.caqi.compliance.domain.entity.PublicacaoPortal;
import br.com.caqi.compliance.domain.repo.PublicacaoPortalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PublicacaoService — snapshot + hash + dedup + chain log")
class PublicacaoServiceTest {

    private PublicacaoPortalRepository repo;
    private PublicSnapshotClient snapshotClient;
    private AuditChainService auditChain;
    private PublicacaoService service;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repo = mock(PublicacaoPortalRepository.class);
        snapshotClient = mock(PublicSnapshotClient.class);
        auditChain = mock(AuditChainService.class);

        AtomicLong idGen = new AtomicLong(0);
        when(repo.save(any(PublicacaoPortal.class))).thenAnswer(inv -> {
            PublicacaoPortal p = inv.getArgument(0);
            p.setId(idGen.incrementAndGet());
            return p;
        });
        when(repo.findFirstByTipoAndReferenciaOrderByDataPublicacaoDesc(anyString(), anyString()))
                .thenReturn(Optional.empty());

        service = new PublicacaoService(repo, snapshotClient, auditChain, "http://portal.exemplo");
    }

    @Test
    @DisplayName("Primeira execução publica todos os 5 tipos quando há conteúdo")
    void primeiraExecucao_publicaTodos() throws Exception {
        when(snapshotClient.fundebExecucao(2025)).thenReturn(node("{\"ano\":2025,\"pctMde\":\"26.50\"}"));
        when(snapshotClient.siopeQuadro(2025)).thenReturn(node("{\"ano\":2025,\"vinculacoes\":{\"mde\":true}}"));
        when(snapshotClient.calculosCaq()).thenReturn(node("[{\"id\":1,\"valorCaqi\":\"4440.17\"}]"));
        when(snapshotClient.contratos()).thenReturn(node("[]"));
        when(snapshotClient.despesas(2025)).thenReturn(node("[{\"id\":1,\"valor\":\"100\"}]"));

        var resultado = service.executarTudo(2025, "test");

        assertThat(resultado.publicadas()).isEqualTo(5);
        assertThat(resultado.skipped()).isZero();
        assertThat(resultado.falhas()).isZero();
        assertThat(resultado.entradas()).extracting(PublicacaoService.EntradaResultado::tipo)
                .containsExactly(
                        PublicacaoService.TIPO_FUNDEB,
                        PublicacaoService.TIPO_SIOPE,
                        PublicacaoService.TIPO_CALCULOS,
                        PublicacaoService.TIPO_CONTRATOS,
                        PublicacaoService.TIPO_DESPESAS);
        verify(repo, times(5)).save(any(PublicacaoPortal.class));
        // 5 publicações + 1 lote = 6 entradas no chain
        verify(auditChain, times(6)).registrar(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Hash idêntico ao último publicado → skip; nada é persistido nem registrado")
    void hashIdentico_skip() throws Exception {
        JsonNode payload = node("{\"ano\":2025,\"pctMde\":\"26.50\"}");
        when(snapshotClient.fundebExecucao(2025)).thenReturn(payload);
        // Outros tipos retornam null para isolar o caso
        when(snapshotClient.siopeQuadro(2025)).thenReturn(null);
        when(snapshotClient.calculosCaq()).thenReturn(null);
        when(snapshotClient.contratos()).thenReturn(null);
        when(snapshotClient.despesas(2025)).thenReturn(null);

        // Pré-seed: já existe publicação anterior com o MESMO hash que o canonical do payload
        String hashCanonical = computarHashCanonical(payload);
        var anterior = new PublicacaoPortal();
        anterior.setId(42L);
        anterior.setTipo(PublicacaoService.TIPO_FUNDEB);
        anterior.setReferencia("2025");
        anterior.setConteudoHash(hashCanonical);
        anterior.setDataPublicacao(LocalDateTime.now().minusHours(6));
        when(repo.findFirstByTipoAndReferenciaOrderByDataPublicacaoDesc(
                PublicacaoService.TIPO_FUNDEB, "2025")).thenReturn(Optional.of(anterior));

        var resultado = service.executarTudo(2025, "test");

        var fundebEntry = resultado.entradas().stream()
                .filter(e -> e.tipo().equals(PublicacaoService.TIPO_FUNDEB))
                .findFirst().orElseThrow();
        assertThat(fundebEntry.status()).isEqualTo(PublicacaoService.Status.SKIPPED);
        assertThat(fundebEntry.publicacaoId()).isEqualTo(42L);
        assertThat(fundebEntry.hash()).isEqualTo(hashCanonical);

        // Nenhum INSERT em publicacao_portal para o tipo Fundeb (apenas backends nulos contam como falha)
        verify(repo, never()).save(any(PublicacaoPortal.class));
    }

    @Test
    @DisplayName("Backend retorna null → status FALHA, demais snapshots seguem normalmente")
    void backendNull_falhaIsolada() throws Exception {
        when(snapshotClient.fundebExecucao(2025)).thenReturn(null);
        when(snapshotClient.siopeQuadro(2025)).thenReturn(node("{\"ok\":true}"));
        when(snapshotClient.calculosCaq()).thenReturn(null);
        when(snapshotClient.contratos()).thenReturn(node("[]"));
        when(snapshotClient.despesas(2025)).thenReturn(null);

        var resultado = service.executarTudo(2025, "test");

        assertThat(resultado.publicadas()).isEqualTo(2); // siope + contratos
        assertThat(resultado.falhas()).isEqualTo(3);
        verify(repo, times(2)).save(any(PublicacaoPortal.class));
    }

    @Test
    @DisplayName("Cliente lança exceção em um snapshot → FALHA isolada, não interrompe lote")
    void clienteExcecao_naoInterrompe() throws Exception {
        when(snapshotClient.fundebExecucao(2025)).thenThrow(new RuntimeException("backend offline"));
        when(snapshotClient.siopeQuadro(2025)).thenReturn(node("{\"ok\":true}"));
        when(snapshotClient.calculosCaq()).thenReturn(node("[]"));
        when(snapshotClient.contratos()).thenReturn(node("[]"));
        when(snapshotClient.despesas(2025)).thenReturn(node("[]"));

        var resultado = service.executarTudo(2025, "test");

        assertThat(resultado.falhas()).isEqualTo(1);
        assertThat(resultado.publicadas()).isEqualTo(4);
        var fundeb = resultado.entradas().stream()
                .filter(e -> e.tipo().equals(PublicacaoService.TIPO_FUNDEB))
                .findFirst().orElseThrow();
        assertThat(fundeb.status()).isEqualTo(PublicacaoService.Status.FALHA);
        assertThat(fundeb.erro()).contains("backend offline");
    }

    @Test
    @DisplayName("Snapshot persistido grava tipo, referencia, hash, tamanho_bytes, url e snapshot JSON")
    void snapshotPersistido_camposCorretos() throws Exception {
        JsonNode payload = node("{\"ano\":2025,\"valor\":\"100.00\"}");
        when(snapshotClient.fundebExecucao(2025)).thenReturn(payload);
        when(snapshotClient.siopeQuadro(2025)).thenReturn(null);
        when(snapshotClient.calculosCaq()).thenReturn(null);
        when(snapshotClient.contratos()).thenReturn(null);
        when(snapshotClient.despesas(2025)).thenReturn(null);

        service.executarTudo(2025, "manual:admin");

        ArgumentCaptor<PublicacaoPortal> cap = ArgumentCaptor.forClass(PublicacaoPortal.class);
        verify(repo, times(1)).save(cap.capture());
        var p = cap.getValue();
        assertThat(p.getTipo()).isEqualTo(PublicacaoService.TIPO_FUNDEB);
        assertThat(p.getReferencia()).isEqualTo("2025");
        assertThat(p.getConteudoHash()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(p.getTamanhoBytes()).isPositive();
        assertThat(p.getUrlPublica()).isEqualTo("http://portal.exemplo/transparencia/fundeb?ano=2025");
        assertThat(p.getSnapshot()).isEqualTo(payload);
    }

    @Test
    @DisplayName("ano=null usa Year.now() — referencia gerada é o ano corrente")
    void anoNull_usaCorrente() throws Exception {
        int corrente = java.time.Year.now().getValue();
        when(snapshotClient.fundebExecucao(corrente)).thenReturn(node("{}"));
        when(snapshotClient.siopeQuadro(corrente)).thenReturn(null);
        when(snapshotClient.calculosCaq()).thenReturn(null);
        when(snapshotClient.contratos()).thenReturn(null);
        when(snapshotClient.despesas(corrente)).thenReturn(null);

        var resultado = service.executarTudo(null, "scheduler");

        assertThat(resultado.ano()).isEqualTo(corrente);
        var fundeb = resultado.entradas().stream()
                .filter(e -> e.tipo().equals(PublicacaoService.TIPO_FUNDEB))
                .findFirst().orElseThrow();
        assertThat(fundeb.referencia()).isEqualTo(String.valueOf(corrente));
    }

    @Test
    @DisplayName("Hash canônico é estável: mesmo conteúdo, ordem de chaves diferente → mesmo SHA-256")
    void hashCanonical_estavelOrdemChaves() throws Exception {
        JsonNode a = node("{\"a\":1,\"b\":2,\"c\":3}");
        JsonNode b = node("{\"c\":3,\"a\":1,\"b\":2}");

        when(snapshotClient.fundebExecucao(2025)).thenReturn(a);
        when(snapshotClient.siopeQuadro(2025)).thenReturn(null);
        when(snapshotClient.calculosCaq()).thenReturn(null);
        when(snapshotClient.contratos()).thenReturn(null);
        when(snapshotClient.despesas(2025)).thenReturn(null);

        var r1 = service.executarTudo(2025, "test1");
        var hash1 = r1.entradas().get(0).hash();

        // Reset repo para nova execução com payload reordenado
        when(snapshotClient.fundebExecucao(2025)).thenReturn(b);
        var r2 = service.executarTudo(2025, "test2");
        var hash2 = r2.entradas().get(0).hash();

        assertThat(hash1).isEqualTo(hash2);
    }

    private JsonNode node(String json) throws Exception {
        return jsonMapper.readTree(json);
    }

    /** Recalcula o que o serviço calcularia internamente, para o teste de skip. */
    private String computarHashCanonical(JsonNode payload) throws Exception {
        var canonicalMapper = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        byte[] bytes = canonicalMapper.writeValueAsBytes(payload);
        return PublicacaoService.sha256Hex(bytes);
    }
}
