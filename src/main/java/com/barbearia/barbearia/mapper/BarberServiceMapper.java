package com.barbearia.barbearia.mapper;

import com.barbearia.barbearia.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.model.BarberService;
import org.springframework.stereotype.Component;

@Component
public class BarberServiceMapper {

    public BarberServiceResponse toDTO(BarberService barberService) {
        if (barberService == null) {
            return null;
        }

        return BarberServiceResponse.builder()
                .id(barberService.getId())
                .nameService(barberService.getNameService())
                .description(barberService.getDescription())
                .durationInMinutes(barberService.getDurationInMinutes())
                .price(barberService.getPrice())
                .build();
    }

    public BarberService toEntity(BarberServiceRequest barberServiceRequest) {
        if (barberServiceRequest == null) {
            return null;
        }

        BarberService barberService = new BarberService();
        barberService.setNameService(barberServiceRequest.getNameService());
        barberService.setDescription(barberServiceRequest.getDescription());
        barberService.setDurationInMinutes(barberServiceRequest.getDurationInMinutes());
        barberService.setPrice(barberServiceRequest.getPrice());

        return barberService;
    }

}
