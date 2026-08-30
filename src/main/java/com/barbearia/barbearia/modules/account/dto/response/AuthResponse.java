package com.barbearia.barbearia.modules.account.dto.response;

import com.barbearia.barbearia.modules.account.model.AppUser.PlatformRole;

public record AuthResponse(
        String accessToken,
        long expiresIn,
        Long userId,
        String email,
        PlatformRole role) {
}
