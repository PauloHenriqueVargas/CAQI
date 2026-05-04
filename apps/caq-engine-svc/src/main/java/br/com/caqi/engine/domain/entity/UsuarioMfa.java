package br.com.caqi.engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuario_mfa")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioMfa {

    @Id
    @Column(length = 64)
    private String username;

    /** Secret de 160 bits codificado em Base32 (RFC 4648). */
    @Column(name = "secret_base32", nullable = false, length = 64)
    private String secretBase32;

    /** False enquanto o usuário não validou o primeiro código TOTP. */
    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "enabled_at")
    private Instant enabledAt;

    @Column(name = "ultima_validacao")
    private Instant ultimaValidacao;

    /** Hashes SHA-256 dos backup codes ainda válidos (one-time-use). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "backup_codes_hashes", columnDefinition = "jsonb")
    private List<String> backupCodesHashes = new ArrayList<>();

    @Column(name = "backup_codes_generated_at")
    private Instant backupCodesGeneratedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (enabled == null) enabled = false;
        if (backupCodesHashes == null) backupCodesHashes = new ArrayList<>();
    }
}
