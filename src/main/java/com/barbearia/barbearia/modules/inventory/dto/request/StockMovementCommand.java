package com.barbearia.barbearia.modules.inventory.dto.request;

import com.barbearia.barbearia.modules.inventory.model.Product;

public record StockMovementCommand(Product product, Integer quantity, String reason) {
}
