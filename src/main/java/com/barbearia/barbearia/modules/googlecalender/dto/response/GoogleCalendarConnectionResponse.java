package com.barbearia.barbearia.modules.googlecalender.dto.response;

import java.time.LocalDateTime;

public record GoogleCalendarConnectionResponse(
        boolean connected,
        String googleEmail,
        String calendarId,
        LocalDateTime tokenExpiresAt
) {
}

