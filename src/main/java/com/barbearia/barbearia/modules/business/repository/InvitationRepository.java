package com.barbearia.barbearia.modules.business.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.modules.business.model.Invitation;
import com.barbearia.barbearia.modules.business.model.Invitation.Status;

public interface InvitationRepository extends JpaRepository<Invitation, Long>{

    Optional<Invitation> findByBusinessIdAndEmailAndStatus(Long businessId, String email, Status status);

    List<Invitation> findByEmailAndStatus(String email, Status status);

    Optional<Invitation> findByIdAndEmailAndStatus(Long id, String email, Status status);
    
}
