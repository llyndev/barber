package com.barbearia.barbearia.modules.googlecalender.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleCalendarConnectRequest(
        @NotBlank(message = "code obrigatorio")
        String code,

        @NotBlank(message = "state obrigatorio")
        String state,

        @NotBlank(message = "redirectUri obrigatorio")
        String redirectUri,

        String calendarId
) {
}


