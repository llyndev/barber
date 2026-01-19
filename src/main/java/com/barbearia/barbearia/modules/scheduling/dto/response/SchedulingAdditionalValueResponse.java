package com.barbearia.barbearia.modules.scheduling.dto.response;

import java.math.BigDecimal;

public record SchedulingAdditionalValueResponse(
    Long id,
    Long barberId,
    String barberName,
    BigDecimal value
) {}
