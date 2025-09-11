package com.barbearia.barbearia.dto.request;

import com.barbearia.barbearia.model.OpeningHours.TypeRule;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OpeningHoursRequest(
        @NotNull(message = "TypeRule cannot be null")
        TypeRule typeRule,

        @NotNull
        DayOfWeek dayOfWeek,

        @NotNull(message = "isOpen flag cannot be null")
        boolean active,

        LocalTime openTime,
        LocalTime closeTime
) {
    @AssertTrue(message = "Opening time must be before closing time")
    private boolean isTimeConsistent() {
        if (active && openTime != null && closeTime != null) {
            return openTime.isBefore(closeTime);
        }

        return true;
    }

    @AssertTrue(message = "Opening and closing times are mandatory when open")
    private boolean areTimesPresentWhenOpen() {
        if (active) {
            return openTime != null && closeTime != null;
        }
        return true;
    }

}
