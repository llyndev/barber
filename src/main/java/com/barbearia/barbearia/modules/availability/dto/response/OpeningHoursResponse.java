package com.barbearia.barbearia.modules.availability.dto.response;

import com.barbearia.barbearia.modules.availability.model.OpeningHours.TypeRule;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OpeningHoursResponse(
        Long id,
        TypeRule typeRule,
        DayOfWeek dayOfWeek,
        boolean active,
        LocalTime openTime,
        LocalTime closeTime
) {
}
