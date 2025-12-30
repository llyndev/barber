package com.barbearia.barbearia.modules.scheduling.dto.request;

import com.barbearia.barbearia.modules.scheduling.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

public record EndSchedulingRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,
        
        @DecimalMin(value = "0.0", message = "Additional value must be positive")
        BigDecimal additionalValue,
        
        String observation,

        List<Long> servicesIds,

        List<ProductUsageRequest> productsUsed
) {
}
