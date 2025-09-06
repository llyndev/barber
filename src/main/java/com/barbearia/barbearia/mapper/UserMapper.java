package com.barbearia.barbearia.mapper;

import com.barbearia.barbearia.dto.response.BarberResponse;
import com.barbearia.barbearia.dto.response.ClientResponse;
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

    public static BarberResponse toBarberResponse(AppUser appUser) {
        if (appUser == null) {
            return null;
        }
        return new BarberResponse(appUser.getId(), appUser.getName());
    }

    public static ClientResponse toClientResponse(AppUser appUser) {
        if (appUser == null) {
            return null;
        }
        return new ClientResponse(appUser.getId(), appUser.getName());
    }
}
