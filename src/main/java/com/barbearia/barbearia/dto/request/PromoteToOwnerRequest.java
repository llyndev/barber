package com.barbearia.barbearia.dto.request;

import com.barbearia.barbearia.model.PlanType;
import java.time.LocalDateTime;

public record PromoteToOwnerRequest(
    PlanType plantType,
    LocalDateTime planExpirationDate
) {
}

