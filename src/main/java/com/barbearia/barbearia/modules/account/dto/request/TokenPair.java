package com.barbearia.barbearia.modules.account.dto.request;

import com.barbearia.barbearia.security.UserDetailsImpl;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserDetailsImpl user
) {
}
