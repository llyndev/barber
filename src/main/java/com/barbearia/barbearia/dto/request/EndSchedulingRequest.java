package com.barbearia.barbearia.dto.request;

import com.barbearia.barbearia.model.PaymentMethod;

import java.math.BigDecimal;

public record EndSchedulingRequest(
        PaymentMethod paymentMethod,
        BigDecimal additionalValue,
        String observation
) {
}
