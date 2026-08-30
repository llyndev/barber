package com.barbearia.barbearia.modules.account.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_jti", columnList = "jti", unique = true),
        @Index(name = "idx_refresh_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant expiresAt;

    // null = ativo. Preenchido no logout, na rotação ou no bloqueio do usuário
    private Instant revokeAt;

    // detecta reuso de token antigo
    private String replacedByJti;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public boolean isActive() {
        return revokeAt == null && expiresAt.isAfter(Instant.now());
    }
}
