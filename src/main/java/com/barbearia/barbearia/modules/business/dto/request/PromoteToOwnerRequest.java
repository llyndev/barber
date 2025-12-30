package com.barbearia.barbearia.modules.business.dto.request;

import com.barbearia.barbearia.modules.business.model.PlanType;
import java.time.LocalDateTime;

public record PromoteToOwnerRequest(
    PlanType plantType,
    LocalDateTime planExpirationDate
) {
}

