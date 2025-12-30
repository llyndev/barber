package com.barbearia.barbearia.modules.availability.dto.request;

import com.barbearia.barbearia.modules.availability.model.OpeningHours.TypeRule;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record SpecificDateRequest(

        @NotNull
        TypeRule typeRule,

        @NotNull
        LocalDate specificDate,

        @NotNull
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
