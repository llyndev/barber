package com.barbearia.barbearia.dto.response;

import com.barbearia.barbearia.model.BusinessRole;

public record UserBusinessResponse(
    Long businessId,
    String businessName,
    String slug,
    BusinessRole role
) {}
