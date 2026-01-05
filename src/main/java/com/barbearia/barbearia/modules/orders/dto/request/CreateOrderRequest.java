package com.barbearia.barbearia.modules.orders.dto.request;

public record CreateOrderRequest(
    Long schedulingId,
    Long clientId,
    String clientName,
    Long professionalId
) {}
