package com.barbearia.barbearia.mapper;

import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.model.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toDTO(AppUser appUser) {
        if (appUser == null) return null;

        return new UserResponse(
                appUser.getId(),
                appUser.getName(),
                appUser.getEmail(),
                appUser.getTelephone(),
                appUser.getRole()
        );
    }
}
