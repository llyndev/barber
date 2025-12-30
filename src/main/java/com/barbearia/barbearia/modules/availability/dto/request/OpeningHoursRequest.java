package com.barbearia.barbearia.modules.availability.dto.request;

import com.barbearia.barbearia.modules.availability.model.OpeningHours.TypeRule;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpeningHoursRequest(
        TypeRule typeRule,

        @NotNull
        DayOfWeek dayOfWeek,

        @NotNull(message = "isOpen flag cannot be null")
        @JsonProperty("isOpen")
        boolean active,

        @JsonProperty("startTime")
        LocalTime openTime,

        @JsonProperty("endTime")
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
