package com.barbearia.barbearia.modules.inventory.dto.request;

import com.barbearia.barbearia.modules.inventory.model.StockMovementType;

public record MovementRequest(
    StockMovementType type,
    Integer quantity,
    String reason
) {
    
}
