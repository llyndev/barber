package com.barbearia.barbearia.modules.orders.dto.request;

import com.barbearia.barbearia.modules.scheduling.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutRequest(
    @NotNull PaymentMethod paymentMethod,
    List<AdditionalValueRequest> additionalValues
) {
    public record AdditionalValueRequest(
        Long barberId,
        BigDecimal value
    ) {}
}
