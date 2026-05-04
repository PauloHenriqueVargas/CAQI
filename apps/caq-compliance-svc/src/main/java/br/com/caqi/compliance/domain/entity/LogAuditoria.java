package br.com.caqi.compliance.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false)
    private String tabela;

    @Column(name = "registro_id", nullable = false)
    private String registroId;

    @Column(nullable = false, length = 10)
    private String acao;

    @Column(name = "carimbo_tempo")
    private LocalDateTime carimboTempo;

    /** Hash hex SHA-256 (64 chars) do log anterior na cadeia. "0..0" para o gênesis. */
    @Column(name = "hash_antes", length = 64)
    private String hashAntes;

    /** SHA-256 hex de (hash_antes || tabela || registro_id || acao || carimbo_tempo). */
    @Column(name = "hash_depois", length = 64)
    private String hashDepois;

    @PrePersist
    void prePersist() {
        if (carimboTempo == null) carimboTempo = LocalDateTime.now();
    }
}
