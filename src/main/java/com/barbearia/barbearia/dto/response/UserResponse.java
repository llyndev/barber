package com.barbearia.barbearia.dto.response;

import com.barbearia.barbearia.model.AppUser;
import java.util.List;

public record UserResponse(
    Long id,
    String name,
    String email,
    String telephone,
    String plantType,
    AppUser.PlatformRole platformRole,
    List<UserBusinessResponse> userBusinesses
){}
