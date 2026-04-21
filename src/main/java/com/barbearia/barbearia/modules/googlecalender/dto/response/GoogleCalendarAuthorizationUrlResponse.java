package com.barbearia.barbearia.modules.googlecalender.dto.response;

import java.time.LocalDateTime;

public record GoogleCalendarAuthorizationUrlResponse(
        String authorizationUrl,
        String state,
        LocalDateTime expiresAt
) {
}

