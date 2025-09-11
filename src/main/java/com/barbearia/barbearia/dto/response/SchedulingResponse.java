package com.barbearia.barbearia.dto.response;

import com.barbearia.barbearia.model.AppointmentStatus;
import com.barbearia.barbearia.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SchedulingResponse(
        Long id,
        LocalDateTime dateTime,
        AppointmentStatus states,
        Long clientId,
        String clientName,
        Long barberId,
        String barberName,
        List<BarberServiceResponse> services,
        ClientResponse client,
        BarberResponse barber,
        String observation,
        BigDecimal additionalValue,
        PaymentMethod paymentMethod
        ) {
}
