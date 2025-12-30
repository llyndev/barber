package com.barbearia.barbearia.modules.business.dto.response;

import com.barbearia.barbearia.modules.business.model.BusinessRole;

public record UserBusinessResponse(
    Long id,
    Long businessId,
    String businessName,
    String slug,
    BusinessRole role,
    UserSummary user
) {
    public record UserSummary(Long id, String name, String email, String profileImage) {}
}
