package com.barbearia.barbearia.modules.account.dto.response;

import com.barbearia.barbearia.modules.business.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.modules.account.model.AppUser;
import java.util.List;

public record UserResponse(
    Long id,
    String name,
    String email,
    String telephone,
    String plantType,
    boolean active,
    AppUser.PlatformRole platformRole,
    String profileImage,
    List<UserBusinessResponse> userBusinesses
){}
