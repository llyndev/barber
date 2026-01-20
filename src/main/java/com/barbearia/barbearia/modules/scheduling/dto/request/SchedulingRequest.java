package com.barbearia.barbearia.modules.scheduling.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record SchedulingRequest(

        @NotNull(message = "The service(s) must be informed.")
        List<Long> barberServiceIds,

        @NotNull(message = "The barber must be informed.")
        Long barberId,

        @NotNull(message = "The date and time must be informed.")
        LocalDateTime dateTime,

        Long clientId,
        String clientName,
        Boolean force

) {}
