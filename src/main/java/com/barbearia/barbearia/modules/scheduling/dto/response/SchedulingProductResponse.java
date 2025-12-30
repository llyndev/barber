package com.barbearia.barbearia.modules.scheduling.dto.response;

import java.math.BigDecimal;

public record SchedulingProductResponse(
    Long productId,
    String productName,
    Integer quantity,
    BigDecimal price
) {}
