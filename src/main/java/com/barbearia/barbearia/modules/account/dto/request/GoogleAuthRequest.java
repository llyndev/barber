package com.barbearia.barbearia.modules.account.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank(message = "idToken obrigatorio")
        String idToken
) {
}


