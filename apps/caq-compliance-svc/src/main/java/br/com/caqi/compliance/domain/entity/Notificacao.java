package br.com.caqi.compliance.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "notificacao")
@Getter
@Setter
@NoArgsConstructor
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacao_id")
    private Long id;

    /** VIOLACAO_MDE_25 | VIOLACAO_FUNDEB_70 | VIOLACAO_VAAT_15 | GAP_CAQ_ALTO | etc. */
    @Column(nullable = false, length = 60)
    private String tipo;

    /** info | warn | alta | critica */
    @Column(nullable = false, length = 20)
    private String severidade;

    @Column(nullable = false)
    private String titulo;

    private String descricao;

    @Column(name = "ano_referencia", nullable = false)
    private Integer anoReferencia;

    @Column(name = "base_legal")
    private String baseLegal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /** aberta | em_analise | resolvida | ignorada */
    @Column(nullable = false, length = 20)
    private String status = "aberta";

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "aberta";
    }
}
