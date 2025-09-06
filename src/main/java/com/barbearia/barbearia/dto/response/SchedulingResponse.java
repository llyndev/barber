package com.barbearia.barbearia.dto.response;

import com.barbearia.barbearia.model.AppointmentStatus;

import java.time.LocalDateTime;

public record SchedulingResponse(Long id,
        LocalDateTime dateTime,
        AppointmentStatus states,
        Long clientId,
        String clientName,
        Long barberId,
        String barberName,
        Long serviceId,
        String serviceName,
        ClientResponse client,
        BarberResponse barber,
        BarberServiceResponse service) {
}
