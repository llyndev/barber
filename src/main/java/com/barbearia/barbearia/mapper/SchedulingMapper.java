package com.barbearia.barbearia.mapper;

import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.model.Scheduling;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchedulingMapper {

    public static SchedulingResponse toResponse(Scheduling scheduling) {

        SchedulingResponse response = new SchedulingResponse();
        response.setId(scheduling.getId());
        response.setDateTime(scheduling.getDateTime());
        response.setClientId(scheduling.getUser().getId());
        response.setClientName(scheduling.getUser().getName());
        response.setBarberId(scheduling.getBarber().getId());
        response.setBarberName(scheduling.getBarber().getName());
        response.setServiceId(scheduling.getBarberService().getId());
        response.setServiceName(scheduling.getBarberService().getNameService());

        return response;
    }

    public static List<SchedulingResponse> toResponseList(List<Scheduling> schedulings) {
        return schedulings.stream()
                .map(SchedulingMapper::toResponse)
                .toList();
    }
}
