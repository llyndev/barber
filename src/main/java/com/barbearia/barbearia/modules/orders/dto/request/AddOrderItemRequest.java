package com.barbearia.barbearia.modules.orders.dto.request;

import com.barbearia.barbearia.modules.orders.model.OrderItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddOrderItemRequest(
    @NotNull OrderItemType type,
    @NotNull Long itemId,
    @Min(1) Integer quantity
) {}
