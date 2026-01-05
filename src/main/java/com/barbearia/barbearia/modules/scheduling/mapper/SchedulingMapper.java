package com.barbearia.barbearia.modules.scheduling.mapper;

import com.barbearia.barbearia.modules.catalog.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.modules.catalog.mapper.BarberServiceMapper;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingProductResponse;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingResponse;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.modules.account.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchedulingMapper {

    public static SchedulingResponse toResponse(Scheduling scheduling) {

        if (scheduling == null) {
            return null;
        }

        List<BarberServiceResponse> serviceResponses = scheduling.getBarberService().stream()
                .map(BarberServiceMapper::toDTO)
                .toList();

        List<SchedulingProductResponse> productResponses = scheduling.getProductsUsed() == null ? 
            List.of() :
            scheduling.getProductsUsed().stream()
                .map(sp -> new SchedulingProductResponse(
                    sp.getProduct().getId(),
                    sp.getProduct().getName(),
                    sp.getQuantity(),
                    sp.getProduct().getPrice()
                ))
                .toList();

        var clientResponse = scheduling.getUser() != null ? UserMapper.toClientResponse(scheduling.getUser()) : null;

        var barberResponse = UserMapper.toBarberResponse(scheduling.getBarber());

        return new SchedulingResponse(
                scheduling.getId(),
                scheduling.getDateTime(),
                scheduling.getStates(),
                scheduling.getUser() != null ? scheduling.getUser().getId() : null,
                scheduling.getUser() != null ? scheduling.getUser().getName() : scheduling.getClientName(),
                scheduling.getUser() != null ? scheduling.getUser().getEmail() : null,
                scheduling.getBarber().getId(),
                scheduling.getBarber().getName(),
                serviceResponses,
                clientResponse,
                barberResponse,
                scheduling.getObservation(),
                scheduling.getAdditionalValue(),
                scheduling.getPaymentMethod(),
                productResponses,
                scheduling.getBusiness() != null ? scheduling.getBusiness().getName() : null,
                scheduling.getBusiness() != null ? scheduling.getBusiness().getSlug() : null
        );
    }

    public static List<SchedulingResponse> toResponseList(List<Scheduling> scheduling) {
        return scheduling.stream()
                .map(SchedulingMapper::toResponse)
                .toList();
    }
}
