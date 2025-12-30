package com.barbearia.barbearia.modules.business.dto.request;

import com.barbearia.barbearia.modules.business.model.BusinessRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddUserToBusinessRequest(
        @NotBlank
        String userEmail,

        @NotNull
        BusinessRole role

) {
    
}
