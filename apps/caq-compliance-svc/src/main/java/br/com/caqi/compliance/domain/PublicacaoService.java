package br.com.caqi.compliance.domain;

import br.com.caqi.compliance.core.client.PublicSnapshotClient;
import br.com.caqi.compliance.domain.entity.PublicacaoPortal;
import br.com.caqi.compliance.domain.repo.PublicacaoPortalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Publicação automática para transparência ativa (LRF art. 48-A).
 *
 * Snapshots tirados:
 *  - fundeb_execucao (referencia=YYYY)
 *  - siope_quadro    (referencia=YYYY)
 *  - calculos_caq    (referencia="-" — atemporal)
 *  - contratos       (referencia="-")
 *  - despesas        (referencia=YYYY)
 *
 * Cada snapshot:
 *  1. Busca conteúdo público via PublicSnapshotClient
 *  2. Serializa JSON canônico (chaves em ordem alfabética, sem pretty-print)
 *  3. Calcula SHA-256 hex
 *  4. Compara com último publicado para o mesmo (tipo, referencia)
 *  5. Se diferente, INSERT na publicacao_portal + LogAuditoria.registrar()
 *  6. Se igual, skip (idempotente)
 *
 * Falhas isoladas em um snapshot não interrompem os demais.
 */
@Service
@Slf4j
public class PublicacaoService {

    public static final String TIPO_FUNDEB    = "fundeb_execucao";
    public static final String TIPO_SIOPE     = "siope_quadro";
    public static final String TIPO_CALCULOS  = "calculos_caq";
    public static final String TIPO_CONTRATOS = "contratos";
    public static final String TIPO_DESPESAS  = "despesas";

    private final PublicacaoPortalRepository repo;
    private final PublicSnapshotClient snapshotClient;
    private final AuditChainService auditChain;
    private final ObjectMapper canonicalMapper;
    private final String basePublicaUrl;

    public PublicacaoService(
            PublicacaoPortalRepository repo,
            PublicSnapshotClient snapshotClient,
            AuditChainService auditChain,
            @Value("${caqi.publicacao.base-url-publica:http://localhost:3000}") String basePublicaUrl
    ) {
        this.repo = repo;
        this.snapshotClient = snapshotClient;
        this.auditChain = auditChain;
        this.basePublicaUrl = basePublicaUrl;
        this.canonicalMapper = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Executa todos os 5 snapshots para o ano informado (ou ano corrente se null).
     * Retorna estatísticas: tentativas/publicadas/skipped/falhas.
     */
    @Transactional
    public ResultadoPublicacao executarTudo(Integer ano, String triggeredBy) {
        int alvo = (ano != null) ? ano : Year.now().getValue();
        String anoStr = String.valueOf(alvo);
        String semRef = "-";

        List<EntradaResultado> resultados = new ArrayList<>();

        resultados.add(executarSnapshot(TIPO_FUNDEB,    anoStr, () -> snapshotClient.fundebExecucao(alvo),
                "/transparencia/fundeb?ano=" + alvo));
        resultados.add(executarSnapshot(TIPO_SIOPE,     anoStr, () -> snapshotClient.siopeQuadro(alvo),
                "/transparencia/fundeb?ano=" + alvo));
        resultados.add(executarSnapshot(TIPO_CALCULOS,  semRef, snapshotClient::calculosCaq,
                "/transparencia/calculos"));
        resultados.add(executarSnapshot(TIPO_CONTRATOS, semRef, snapshotClient::contratos,
                "/transparencia/contratos"));
        resultados.add(executarSnapshot(TIPO_DESPESAS,  anoStr, () -> snapshotClient.despesas(alvo),
                "/transparencia/despesas?ano=" + alvo));

        log.info("Publicação LRF art. 48-A — ano={} trigger={} resultados={}", alvo, triggeredBy, resultados);

        // Registra a execução no log de auditoria (chain SHA-256)
        try {
            auditChain.registrar("publicacao_portal", anoStr, "executar_lote_" + triggeredBy, null);
        } catch (Exception e) {
            log.warn("Falha registrando auditoria do lote de publicação", e);
        }

        return new ResultadoPublicacao(alvo, triggeredBy, LocalDateTime.now(), resultados);
    }

    private EntradaResultado executarSnapshot(String tipo, String referencia,
                                               Supplier<JsonNode> fonte, String urlRelativa) {
        try {
            JsonNode payload = fonte.get();
            if (payload == null) {
                log.warn("[publicacao] {} ref={} — backend retornou null, skip", tipo, referencia);
                return new EntradaResultado(tipo, referencia, Status.FALHA, null, null, null, "backend retornou null");
            }
            byte[] canonical = canonicalMapper.writeValueAsBytes(payload);
            String hash = sha256Hex(canonical);

            Optional<PublicacaoPortal> ultima =
                    repo.findFirstByTipoAndReferenciaOrderByDataPublicacaoDesc(tipo, referencia);

            if (ultima.isPresent() && hash.equals(ultima.get().getConteudoHash())) {
                log.debug("[publicacao] {} ref={} hash idêntico ao último, skip", tipo, referencia);
                return new EntradaResultado(tipo, referencia, Status.SKIPPED,
                        ultima.get().getId(), hash, (long) canonical.length, null);
            }

            PublicacaoPortal nova = new PublicacaoPortal();
            nova.setTipo(tipo);
            nova.setReferencia(referencia);
            nova.setConteudoHash(hash);
            nova.setTamanhoBytes((long) canonical.length);
            nova.setUrlPublica(basePublicaUrl + urlRelativa);
            nova.setDataPublicacao(LocalDateTime.now());
            nova.setSnapshot(payload);
            nova = repo.save(nova);

            try {
                auditChain.registrar("publicacao_portal", String.valueOf(nova.getId()), "publicar:" + tipo, null);
            } catch (Exception e) {
                log.warn("Falha registrando auditoria de publicação id={}", nova.getId(), e);
            }

            log.info("[publicacao] {} ref={} PUBLICADO id={} hash={} bytes={}",
                    tipo, referencia, nova.getId(), hash.substring(0, 12) + "…", canonical.length);

            return new EntradaResultado(tipo, referencia, Status.PUBLICADO,
                    nova.getId(), hash, (long) canonical.length, null);
        } catch (Exception e) {
            log.error("[publicacao] {} ref={} FALHOU: {}", tipo, referencia, e.toString());
            return new EntradaResultado(tipo, referencia, Status.FALHA, null, null, null, e.toString());
        }
    }

    static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /** Visível em test apenas. */
    String sha256HexFromString(String s) {
        return sha256Hex(s.getBytes(StandardCharsets.UTF_8));
    }

    public enum Status { PUBLICADO, SKIPPED, FALHA }

    public record EntradaResultado(
            String tipo,
            String referencia,
            Status status,
            Long publicacaoId,
            String hash,
            Long tamanhoBytes,
            String erro
    ) {}

    public record ResultadoPublicacao(
            int ano,
            String triggeredBy,
            LocalDateTime executadoEm,
            List<EntradaResultado> entradas
    ) {
        public long publicadas() { return entradas.stream().filter(e -> e.status == Status.PUBLICADO).count(); }
        public long skipped()    { return entradas.stream().filter(e -> e.status == Status.SKIPPED).count(); }
        public long falhas()     { return entradas.stream().filter(e -> e.status == Status.FALHA).count(); }
    }
}
