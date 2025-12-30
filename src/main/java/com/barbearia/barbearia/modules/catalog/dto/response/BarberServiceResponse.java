package com.barbearia.barbearia.modules.catalog.dto.response;

import java.math.BigDecimal;

public record BarberServiceResponse(
        Long id,
        String nameService,
        String description,
        Integer durationInMinutes,
        BigDecimal price) {
}
