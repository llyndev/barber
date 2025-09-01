package com.barbearia.barbearia.dto.response;

import java.time.LocalDateTime;

public record SchedulingResponse(Long id,
        LocalDateTime dateTime,
        Long clientId,
        String clientName,
        Long barberId,
        String barberName,
        Long serviceId,
        String serviceName) {
}
