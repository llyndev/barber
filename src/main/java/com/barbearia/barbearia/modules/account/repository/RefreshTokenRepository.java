package com.barbearia.barbearia.modules.account.repository;

import com.barbearia.barbearia.modules.account.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :now " +
            "WHERE r.userId = :userId AND r.revokedAt is NULL")
    int revokedAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
