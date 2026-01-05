package com.barbearia.barbearia.modules.business.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.modules.business.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.modules.business.model.UserBusiness;

@Component
public class UserBusinessMapper {

    public UserBusinessResponse toResponse(UserBusiness user) {
        if (user == null) {
            return null;
        }

        return new UserBusinessResponse(
                user.getId(),
                user.getBusiness().getId(),
                user.getBusiness().getName(),
                user.getBusiness().getSlug(),
                user.getRole(),
                
                new UserBusinessResponse.UserSummary(
                    user.getUser().getId(),
                    user.getUser().getName(),
                    user.getUser().getEmail(),
                    user.getUser().getProfileImage()
                )
        );
    }
    
}
