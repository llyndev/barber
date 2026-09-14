package com.barbearia.barbearia.modules.scheduling.dto.request;

import com.barbearia.barbearia.modules.scheduling.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EndSchedulingRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @Valid
        List<AdditionalValueRequest> additionalValue,
        
        String observation,

        List<Long> servicesIds,

        List<ProductUsageRequest> productsUsed
) {
}
