package com.barbearia.barbearia.modules.orders.dto.response;

import com.barbearia.barbearia.modules.orders.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    Long businessId,
    Long clientId,
    String clientName,
    Long professionalId,
    String professionalName,
    Long schedulingId,
    OrderStatus status,
    BigDecimal totalAmount,
    List<OrderItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
