package com.barbearia.barbearia.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.model.UserBusiness;

@Component
public class UserBusinessMapper {

    public UserBusinessResponse toResponse(UserBusiness user) {
        if (user == null) {
            return null;
        }

        return new UserBusinessResponse(
                user.getBusiness().getId(),
                user.getBusiness().getName(),
                user.getBusiness().getSlug(),
                user.getRole()
        );
    }
    
}
