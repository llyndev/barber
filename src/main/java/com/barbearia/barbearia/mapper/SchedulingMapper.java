package com.barbearia.barbearia.mapper;

import com.barbearia.barbearia.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.model.Scheduling;
import lombok.RequiredArgsConstructor;
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

        var clientResponse = UserMapper.toClientResponse(scheduling.getUser());

        var barberResponse = UserMapper.toBarberResponse(scheduling.getBarber());

        return new SchedulingResponse(
                scheduling.getId(),
                scheduling.getDateTime(),
                scheduling.getStates(),
                scheduling.getUser().getId(),
                scheduling.getUser().getName(),
                scheduling.getBarber().getId(),
                scheduling.getBarber().getName(),
                serviceResponses,
                clientResponse,
                barberResponse,
                scheduling.getObservation(),
                scheduling.getAdditionalValue(),
                scheduling.getPaymentMethod()
        );
    }

    public static List<SchedulingResponse> toResponseList(List<Scheduling> scheduling) {
        return scheduling.stream()
                .map(SchedulingMapper::toResponse)
                .toList();
    }
}
