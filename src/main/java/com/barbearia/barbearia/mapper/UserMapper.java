package com.barbearia.barbearia.mapper;

import com.barbearia.barbearia.dto.response.BarberResponse;
import com.barbearia.barbearia.dto.response.ClientResponse;
import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.model.AppUser;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import java.util.Collections;

@Component
public class UserMapper {

    public UserResponse toDTO(AppUser appUser) {
        if (appUser == null) return null;

        var userBusinesses = appUser.getUserBusinesses() == null ? 
            Collections.<UserBusinessResponse>emptyList() :
            appUser.getUserBusinesses().stream()
                .map(ub -> new UserBusinessResponse(
                    ub.getBusiness().getId(),
                    ub.getBusiness().getName(),
                    ub.getBusiness().getSlug(),
                    ub.getRole()
                ))
                .collect(Collectors.toList());

        return new UserResponse(
                appUser.getId(),
                appUser.getName(),
                appUser.getEmail(),
                appUser.getTelephone(),
                appUser.getPlantType(),
                appUser.getPlatformRole(),
                userBusinesses
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
