package com.barbearia.barbearia.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.dto.response.InvitationResponse;
import com.barbearia.barbearia.model.Invitation;

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
