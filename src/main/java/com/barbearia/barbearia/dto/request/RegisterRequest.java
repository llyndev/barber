package com.barbearia.barbearia.dto.request;

import com.barbearia.barbearia.model.AppUser;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank
        String name,

        @NotBlank
        String email,

        @NotBlank
        String telephone,

        @NotBlank
        String password,

        @NotBlank
        String confirmPassword,

        AppUser.PlatformRole platformRole


) {
}
