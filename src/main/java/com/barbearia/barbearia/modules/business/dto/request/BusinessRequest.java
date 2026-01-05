package com.barbearia.barbearia.modules.business.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusinessRequest(
        @NotBlank(message = "Business name is required")
        @Size(max = 140, message = "Business name must not exceed 140 characters")
        String name,
        
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,
        
        String telephone,
        
        @Size(max = 120, message = "Slug must not exceed 120 characters")
        String slug,

        List<String> amenities,

        String instagramLink,
        
        @Size(min = 8, max = 8, message = "CEP must have 8 digits")
        String cep,
        
        String numero,
        
        String complemento
) {

}
