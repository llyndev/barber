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

        return new BarberServiceResponse(
                barberService.getId(),
                barberService.getNameService(),
                barberService.getDescription(),
                barberService.getDurationInMinutes(),
                barberService.getPrice()
                );
    }

    public BarberService toEntity(BarberServiceRequest barberServiceRequest) {
        if (barberServiceRequest == null) {
            return null;
        }

        BarberService barberService = new BarberService();
        barberService.setNameService(barberServiceRequest.nameService());
        barberService.setDescription(barberServiceRequest.description());
        barberService.setDurationInMinutes(barberServiceRequest.durationInMinutes());
        barberService.setPrice(barberServiceRequest.price());

        return barberService;
    }

}
