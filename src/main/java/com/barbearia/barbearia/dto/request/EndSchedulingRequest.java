package com.barbearia.barbearia.dto.request;

import com.barbearia.barbearia.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record EndSchedulingRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,
        
        @DecimalMin(value = "0.0", message = "Additional value must be positive")
        BigDecimal additionalValue,
        
        String observation
) {
}
