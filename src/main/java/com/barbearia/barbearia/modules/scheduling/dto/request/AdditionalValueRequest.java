package com.barbearia.barbearia.modules.scheduling.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdditionalValueRequest(
        @NotNull
        Long barberId,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal value
) {
}
