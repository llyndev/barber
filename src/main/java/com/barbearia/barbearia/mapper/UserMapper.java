package com.barbearia.barbearia.mapper;

import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.model.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toDTO(AppUser appUser) {
        if (appUser == null) return null;

        return UserResponse.builder()
                .id(appUser.getId())
                .name(appUser.getName())
                .email(appUser.getEmail())
                .telephone(appUser.getTelephone())
                .role(appUser.getRole().name())
                .build();
    }
}
