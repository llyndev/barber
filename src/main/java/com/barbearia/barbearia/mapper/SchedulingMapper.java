package com.barbearia.barbearia.mapper;

import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.model.Scheduling;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchedulingMapper {

    public static SchedulingResponse toResponse(Scheduling scheduling) {

        return new SchedulingResponse(
                scheduling.getId(),
                scheduling.getDateTime(),
                scheduling.getUser().getId(),
                scheduling.getUser().getName(),
                scheduling.getBarber().getId(),
                scheduling.getBarber().getName(),
                scheduling.getBarberService().getId(),
                scheduling.getBarberService().getNameService()
        );
    }

    public static List<SchedulingResponse> toResponseList(List<Scheduling> scheduling) {
        return scheduling.stream()
                .map(SchedulingMapper::toResponse)
                .toList();
    }
}
