package com.barbearia.barbearia.modules.inventory.dto.response;

import java.math.BigDecimal;

public record PublicProdutResponse(
        Long id,
        String name,
        String description,
        boolean available,
        BigDecimal price
) {
}
