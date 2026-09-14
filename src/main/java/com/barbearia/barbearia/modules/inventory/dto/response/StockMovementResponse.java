package com.barbearia.barbearia.modules.inventory.dto.response;

import java.time.LocalDateTime;

import com.barbearia.barbearia.modules.inventory.model.StockMovementType;

public record StockMovementResponse(
        Long id,
        LocalDateTime date,
        String productName,
        StockMovementType type,
        Integer quantity,
        Integer previousQuantity,
        Integer newQuantity,
        String reason,
        String performedBy
) {}
