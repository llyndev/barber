package com.barbearia.barbearia.modules.catalog.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddServiceRequest(
        @NotNull(message = "Service IDs list cannot be null")
        @NotEmpty(message = "At least one service must be provided")
        List<Long> barberServiceIds
) {
}
