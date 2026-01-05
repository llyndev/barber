package com.barbearia.barbearia.modules.orders.dto.response;

import com.barbearia.barbearia.modules.orders.model.OrderItemType;
import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    OrderItemType type,
    Long itemId,
    String name,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) {}
