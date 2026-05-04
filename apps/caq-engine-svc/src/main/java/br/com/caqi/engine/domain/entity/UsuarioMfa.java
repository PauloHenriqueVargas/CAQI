package br.com.caqi.engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (enabled == null) enabled = false;
    }
}
