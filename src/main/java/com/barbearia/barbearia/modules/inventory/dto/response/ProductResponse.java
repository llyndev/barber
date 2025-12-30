package com.barbearia.barbearia.modules.inventory.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
    Long id,
    String name,
    String description,
    String sku,
    Integer quantity,
    Integer minQuantity,
    BigDecimal price,
    BigDecimal costPrice,
    Boolean active
) {
}
