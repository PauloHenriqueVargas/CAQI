package br.com.caqi.compliance.domain;

import br.com.caqi.compliance.domain.entity.LogAuditoria;
import br.com.caqi.compliance.domain.repo.LogAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Cadeia SHA-256 (Merkle-style) sobre log_auditoria.
 *
 * Cada log:
 *   hash_antes  = hash_depois do log anterior (ou 64 zeros para o gênesis)
 *   hash_depois = SHA-256( hash_antes || "|" || tabela || "|" || registro_id || "|" || acao || "|" || carimbo_tempo )
 *
 * Adulteração de qualquer log invalida todos os hash_depois subsequentes — detectável em
 * verificarIntegridade() por comparação contra os hashes recalculados.
 *
 * Concorrência: lock pessimista (FOR UPDATE) no último log antes de calcular o próximo,
 * impedindo race condition em insert simultâneo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditChainService {

    public static final String GENESIS_HASH = "0".repeat(64);

    private final LogAuditoriaRepository repo;

    @Transactional
    public LogAuditoria registrar(String tabela, String registroId, String acao, Long usuarioId) {
        String hashAntes = repo.findUltimoComLock()
                .map(LogAuditoria::getHashDepois)
                .orElse(GENESIS_HASH);

        LogAuditoria entry = new LogAuditoria();
        entry.setTabela(tabela);
        entry.setRegistroId(registroId);
        entry.setAcao(acao);
        entry.setUsuarioId(usuarioId);
        entry.setHashAntes(hashAntes);
        // PrePersist seta carimboTempo se nulo
        if (entry.getCarimboTempo() == null) {
            entry.setCarimboTempo(java.time.LocalDateTime.now());
        }
        entry.setHashDepois(computarHash(hashAntes, tabela, registroId, acao,
                entry.getCarimboTempo().toString()));

        return repo.save(entry);
    }

    /**
     * Recalcula a cadeia inteira e compara com os hashes persistidos.
     * Devolve o id do PRIMEIRO log com inconsistência ou Optional.empty() se íntegro.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Long> verificarIntegridade() {
        List<LogAuditoria> todos = repo.findAllByOrderByIdAsc();
        String prevHash = GENESIS_HASH;
        for (LogAuditoria l : todos) {
            if (!prevHash.equals(l.getHashAntes())) {
                log.warn("Quebra na cadeia: log {} hash_antes={} esperado={}", l.getId(), l.getHashAntes(), prevHash);
                return java.util.Optional.of(l.getId());
            }
            String esperado = computarHash(l.getHashAntes(), l.getTabela(), l.getRegistroId(),
                    l.getAcao(), l.getCarimboTempo().toString());
            if (!esperado.equals(l.getHashDepois())) {
                log.warn("Quebra na cadeia: log {} hash_depois={} esperado={}", l.getId(), l.getHashDepois(), esperado);
                return java.util.Optional.of(l.getId());
            }
            prevHash = l.getHashDepois();
        }
        return java.util.Optional.empty();
    }

    static String computarHash(String hashAntes, String tabela, String registroId, String acao, String carimboTempo) {
        String input = String.join("|", hashAntes, tabela, registroId, acao, carimboTempo);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
