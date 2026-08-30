package com.barbearia.barbearia.modules.account.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        int expiresIn
) {
}
