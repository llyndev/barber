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

    long countByBusinessIdAndRoleAndStatusAndExpiresAtAfter(Long businessId,
                                                            BusinessRole role,
                                                            InvitationStatus status,
                                                            Instant now);
    
}
