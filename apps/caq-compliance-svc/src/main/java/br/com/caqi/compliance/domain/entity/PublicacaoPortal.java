package br.com.caqi.compliance.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entrada do registro público de transparência ativa (LRF art. 48-A).
 *
 * Cada linha representa uma "publicação" — um snapshot imutável de algum
 * dado público (Fundeb, SIOPE, cálculos CAQ, notificações de compliance)
 * em um momento específico, junto com seu hash SHA-256 para verificação
 * de integridade.
 *
 * Idempotência: dedup por (tipo, referencia, conteudo_hash) — re-execução
 * com conteúdo idêntico não cria nova entrada.
 */
@Entity
@Table(name = "publicacao_portal")
@Getter
@Setter
@NoArgsConstructor
public class PublicacaoPortal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "publicacao_id")
    private Long id;

    @Column(name = "escola_id")
    private Long escolaId;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(length = 20)
    private String referencia;

    @Column(name = "conteudo_hash", length = 64)
    private String conteudoHash;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "url_publica")
    private String urlPublica;

    @Column(name = "data_publicacao")
    private LocalDateTime dataPublicacao = LocalDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode snapshot;
}
