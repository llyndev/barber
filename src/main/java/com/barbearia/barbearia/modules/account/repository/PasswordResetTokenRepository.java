package com.barbearia.barbearia.modules.account.repository;

import com.barbearia.barbearia.modules.account.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Modifying
    @Query("update PasswordResetToken t set t.used = true where t.user.id = :userId and t.used = false")
    int invalidateAllActiveByUser(@Param("userId") Long userId);

    Optional<PasswordResetToken> findByToken(String token);
}
