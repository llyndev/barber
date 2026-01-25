package com.barbearia.barbearia.modules.account.dto.request;

import com.barbearia.barbearia.modules.account.model.AppUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        String name,

        @NotBlank
        String email,

        @NotBlank
        String telephone,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        @NotBlank
        String confirmPassword,

        AppUser.PlatformRole platformRole


) {
}
