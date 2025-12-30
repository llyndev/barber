package com.barbearia.barbearia.modules.inventory.dto.request;

import java.math.BigDecimal;

public record ProductRequest(
    String name,
    String description,
    String sku,
    Integer quantity,
    Integer minQuantity,
    BigDecimal price,
    BigDecimal costPrice
) {
    
}
