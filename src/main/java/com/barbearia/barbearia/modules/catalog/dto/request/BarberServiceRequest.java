package com.barbearia.barbearia.modules.catalog.dto.request;

import java.math.BigDecimal;

public record BarberServiceRequest(
        String nameService,
        String description,
        Integer durationInMinutes,
        BigDecimal price) {}
