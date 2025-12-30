package com.barbearia.barbearia.modules.account.dto.request;

import com.barbearia.barbearia.modules.account.model.AppUser;
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
