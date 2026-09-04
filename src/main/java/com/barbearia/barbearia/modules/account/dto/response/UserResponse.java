package com.barbearia.barbearia.modules.account.dto.response;

import com.barbearia.barbearia.modules.account.model.PlatformRole;
import com.barbearia.barbearia.modules.business.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.model.PlanType;

import java.util.List;

public record UserResponse(
        Long id,
        String name,
        String email,
        String telephone,
        PlanType plantType,
        boolean active,
        PlatformRole platformRole,
        String profileImage,
        List<UserBusinessResponse> userBusinesses
){}
