package com.barbearia.barbearia.modules.account.mapper;

import com.barbearia.barbearia.modules.business.dto.response.BarberResponse;
import com.barbearia.barbearia.modules.account.dto.response.ClientResponse;
import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
import com.barbearia.barbearia.modules.business.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.modules.account.model.AppUser;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import java.util.Collections;

@Component
public class UserMapper {

    private static final String BASE_URL = "/uploads/";

    public UserResponse toDTO(AppUser appUser) {
        if (appUser == null) return null;

        var userBusinesses = appUser.getUserBusinesses() == null ? 
            Collections.<UserBusinessResponse>emptyList() :
            appUser.getUserBusinesses().stream()
                .map(ub -> new UserBusinessResponse(
                    ub.getId(),
                    ub.getBusiness().getId(),
                    ub.getBusiness().getName(),
                    ub.getBusiness().getSlug(),
                    ub.getRole(),
                    new UserBusinessResponse.UserSummary(
                        appUser.getId(),
                        appUser.getName(),
                        appUser.getEmail(),
                        appUser.getProfileImage()
                    )
                ))
                .collect(Collectors.toList());

        String imageUrl = null;
        if (appUser.getProfileImage() != null && !appUser.getProfileImage().isBlank()) {
            imageUrl = BASE_URL + appUser.getProfileImage();
        }

        return new UserResponse(
                appUser.getId(),
                appUser.getName(),
                appUser.getEmail(),
                appUser.getTelephone(),
                appUser.getPlantType(),
                appUser.isActive(),
                appUser.getPlatformRole(),
                imageUrl,
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
