package com.barbearia.barbearia.modules.scheduling.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record SchedulingStaffRequest(

        @NotNull(message = "The service(s) must be informed.")
        List<Long> barberServiceIds,

        @NotNull(message = "The barber must be informed.")
        Long barberId,

        @NotNull(message = "The date and time must be informed.")
        LocalDateTime dateTime,

        @NotNull(message = "The client name must be informed.")
        String clientName,

        @NotNull(message = "The client number is required.")
        String clientNumber

) {
}
