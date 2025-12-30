package com.barbearia.barbearia.modules.availability.dto.response;

import com.barbearia.barbearia.modules.availability.model.OpeningHours.TypeRule;

import java.time.LocalDate;
import java.time.LocalTime;

public record SpecificDateResponse(
        Long id,
        LocalDate specificDate,
        TypeRule typeRule,
        boolean active,
        LocalTime openTime,
        LocalTime closeTime
) {
}
