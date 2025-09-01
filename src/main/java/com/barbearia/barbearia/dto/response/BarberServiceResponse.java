package com.barbearia.barbearia.dto.response;

import java.math.BigDecimal;

public record BarberServiceResponse(Long id,
        String nameService,
        String description,
        int durationInMinutes,
        BigDecimal price) {
}
