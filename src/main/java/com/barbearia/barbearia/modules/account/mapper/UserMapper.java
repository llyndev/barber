package com.barbearia.barbearia.modules.account.mapper;

import com.barbearia.barbearia.modules.account.dto.response.MyMembershipResponse;
import com.barbearia.barbearia.modules.business.dto.response.BarberResponse;
import com.barbearia.barbearia.modules.account.dto.response.ClientResponse;
import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
import com.barbearia.barbearia.modules.business.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.UserBusiness;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    private static final String BASE_URL = "/uploads/";

    public UserResponse toDTO(AppUser appUser) {
        if (appUser == null) return null;

        return new UserResponse(
                appUser.getId(),
                appUser.getName(),
                appUser.getEmail(),
                appUser.getTelephone(),
                appUser.getPlantType(),
                appUser.isActive(),
                appUser.getPlatformRole(),
                buildImageUrl(appUser.getProfileImage()),
                List.of()
        );
    }

    public UserResponse toDTOWithBusinesses(AppUser appUser) {
        if (appUser == null) return null;

        var memberships = appUser.getUserBusinesses() == null
                ? List.<MyMembershipResponse>of ()
                : appUser.getUserBusinesses().stream()
                .map(this::toMembership)
                .toList();

        return new UserResponse(
                appUser.getId(),
                appUser.getName(),
                appUser.getEmail(),
                appUser.getTelephone(),
                appUser.getPlantType(),
                appUser.isActive(),
                appUser.getPlatformRole(),
                buildImageUrl(appUser.getProfileImage()),
                memberships
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

    private MyMembershipResponse toMembership(UserBusiness ub) {
        Business business = ub.getBusiness();
        return new MyMembershipResponse(
                ub.getId(),
                business.getId(),
                business.getName(),
                business.getSlug(),
                ub.getRole(),
                ub.getCommissionPercentage(),
                business.isActive()
        );
    }

    private String buildImageUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;

        return BASE_URL + fileName;
    }
}
