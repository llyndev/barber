package com.barbearia.barbearia.modules.inventory.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.modules.inventory.dto.response.StockMovementResponse;
import com.barbearia.barbearia.modules.inventory.model.StockMovement;

@Component
public class StockMovementMapper {

    public StockMovementResponse toResponse(StockMovement movement) {
        if (movement == null) {
            return null;
        }

        String performedBy = movement.getPerformedBy() != null ? movement.getPerformedBy().getUser().getName() : "Sistema";

        return new StockMovementResponse(
            movement.getId(),
            movement.getOccurredAt(),
            movement.getProduct().getName(),
            movement.getType(),
            movement.getQuantity(),
            movement.getPreviousQuantity(),
            movement.getNewQuantity(),
            movement.getReason(),
            performedBy
        );
    }
}
