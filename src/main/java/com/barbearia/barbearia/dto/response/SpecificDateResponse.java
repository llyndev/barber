package com.barbearia.barbearia.dto.response;

import com.barbearia.barbearia.model.OpeningHours.TypeRule;

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
