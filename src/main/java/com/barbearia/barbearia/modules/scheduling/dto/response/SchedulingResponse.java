package com.barbearia.barbearia.modules.scheduling.dto.response;

import com.barbearia.barbearia.modules.business.dto.response.BarberResponse;
import com.barbearia.barbearia.modules.catalog.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.modules.scheduling.model.AppointmentStatus;
import com.barbearia.barbearia.modules.scheduling.model.PaymentMethod;
import com.barbearia.barbearia.modules.account.dto.response.ClientResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SchedulingResponse(
        Long id,
        LocalDateTime dateTime,
        AppointmentStatus states,
        Long clientId,
        String clientName,
        String clientEmail,
        Long barberId,
        String barberName,
        List<BarberServiceResponse> services,
        ClientResponse client,
        BarberResponse barber,
        String observation,
        BigDecimal additionalValue,
        List<SchedulingAdditionalValueResponse> additionalValues,
        PaymentMethod paymentMethod,
        List<SchedulingProductResponse> productsUsed,
        String businessName,
        String businessSlug
        ) {
}
