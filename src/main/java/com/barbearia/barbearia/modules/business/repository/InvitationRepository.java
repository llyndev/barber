package com.barbearia.barbearia.modules.business.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.business.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.modules.business.model.Invitation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long>{

    Optional<Invitation> findByBusinessIdAndEmailAndStatus(Long businessId, String email, InvitationStatus status);

    List<Invitation> findByEmailAndStatus(String email, InvitationStatus status);

    Optional<Invitation> findByIdAndEmailAndStatus(Long id, String email, InvitationStatus status);

    @Query("""
            SELECT count(i) FROM invitations i
            WHERE i.business.id = :businessId
            AND i.role = :role
            AND i.status = com.barbearia.barbearia.modules.business.model.InvitationStatus.PENDING
            AND i.expiresAt > :now
            """)
    long countPendingByBusinessIdAndRole(@Param("businessId") Long businessId,
                                         @Param("role") BusinessRole role,
                                         @Param("now") Instant now);
    
}
