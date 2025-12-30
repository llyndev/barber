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

        String userName = movement.getUser() != null ? movement.getUser().getName() : "Sistema";

        return new StockMovementResponse(
            movement.getId(),
            movement.getDate(),
            movement.getProduct().getName(),
            movement.getType(),
            movement.getQuantity(),
            movement.getReason(),
            userName
        );
    }
}
