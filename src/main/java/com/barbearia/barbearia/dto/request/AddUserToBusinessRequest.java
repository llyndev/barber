package com.barbearia.barbearia.dto.request;

import com.barbearia.barbearia.model.BusinessRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddUserToBusinessRequest(
        @NotBlank
        String userEmail,

        @NotNull
        BusinessRole role

) {
    
}
