package com.barbearia.barbearia.modules.business.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.modules.business.dto.response.InvitationResponse;
import com.barbearia.barbearia.modules.business.model.Invitation;

@Component
public class InvitationMapper {
    
    public InvitationResponse toResponse(Invitation invitation) {
        if (invitation == null) {
            return null;
        }

        return new InvitationResponse(
                invitation.getId(),
                invitation.getBusiness().getId(),
                invitation.getBusiness().getName(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt()
        );
    }

}
