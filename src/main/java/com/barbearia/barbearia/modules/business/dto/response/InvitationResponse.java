package com.barbearia.barbearia.modules.business.dto.response;

import java.time.Instant;

import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.business.model.Invitation;

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
