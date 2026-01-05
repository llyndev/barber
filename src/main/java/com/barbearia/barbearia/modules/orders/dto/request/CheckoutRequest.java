package com.barbearia.barbearia.modules.orders.dto.request;

import com.barbearia.barbearia.modules.scheduling.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
    @NotNull PaymentMethod paymentMethod
) {}
