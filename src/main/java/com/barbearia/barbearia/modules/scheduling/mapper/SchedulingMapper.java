package com.barbearia.barbearia.modules.scheduling.mapper;

import com.barbearia.barbearia.modules.catalog.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.modules.catalog.mapper.BarberServiceMapper;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingAdditionalValueResponse;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingProductResponse;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingResponse;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.modules.account.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SchedulingMapper {

    private final BarberServiceMapper barberServiceMapper;

    public SchedulingResponse toResponse(Scheduling scheduling) {

        if (scheduling == null) {
            return null;
        }

        List<BarberServiceResponse> serviceResponses = scheduling.getBarberService() == null ? List.of()
                : scheduling.getBarberService().stream()
                        .map(barberServiceMapper::toDTO)
                        .toList();

        List<SchedulingProductResponse> productResponses = scheduling.getProductsUsed() == null ? List.of()
            : scheduling.getProductsUsed().stream()
                .map(sp -> new SchedulingProductResponse(
                    sp.getProduct().getId(),
                    sp.getProduct().getName(),
                    sp.getQuantity(),
                    sp.getProduct().getPrice()
                ))
                .toList();

        List<SchedulingAdditionalValueResponse> additionalValueResponses = scheduling.getAdditionalValue().stream()
                .map(av -> new SchedulingAdditionalValueResponse(
                        av.getId(),
                        av.getBarber().getId(),
                        av.getBarber().getName(),
                        av.getValue()
                ))
                .toList();

        var user = scheduling.getUser();
        var barber = scheduling.getBarber();
        var business = scheduling.getBusiness();

        return new SchedulingResponse(
                scheduling.getId(),
                scheduling.getDateTime(),
                scheduling.getStates(),
                scheduling.getUser() != null ? scheduling.getUser().getId() : null,
                scheduling.getUser() != null ? scheduling.getUser().getName() : scheduling.getClientName(),
                scheduling.getUser() != null ? scheduling.getUser().getEmail() : null,
                barber != null ? barber.getId() : null,
                barber != null ? barber.getName() : null,
                serviceResponses,
                user != null ? UserMapper.toClientResponse(user) : null,
                barber != null ? UserMapper.toBarberResponse(barber) : null,
                scheduling.getObservation(),
                scheduling.getTotalAdditionalValue(),
                additionalValueResponses,
                scheduling.getPaymentMethod(),
                productResponses,
                business != null ? business.getName() : null,
                business != null ? business.getSlug() : null
        );
    }

    public List<SchedulingResponse> toResponseList(List<Scheduling> scheduling) {
        return scheduling.stream().map(this::toResponse).toList();
    }
}
