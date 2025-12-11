package com.barbearia.barbearia.dto.response;

import java.time.Instant;

import com.barbearia.barbearia.model.BusinessRole;
import com.barbearia.barbearia.model.Invitation;

public record InvitationResponse(
        Long id,
        Long businessId,
        String businessName,
        String email,
        BusinessRole role,
        Invitation.Status status,
        Instant expiresAt
) {
    
}
