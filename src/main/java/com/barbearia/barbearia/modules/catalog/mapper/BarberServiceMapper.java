package com.barbearia.barbearia.modules.catalog.mapper;

import com.barbearia.barbearia.modules.catalog.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.modules.catalog.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.modules.catalog.model.BarberService;
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

    public void updateEntityFromRequest(BarberServiceRequest request, BarberService entity) {
        if (request.nameService() != null) {
            entity.setNameService(request.nameService());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.durationInMinutes() != null) {
            entity.setDurationInMinutes(request.durationInMinutes());
        }
        if (request.price() != null) {
            entity.setPrice(request.price());
        }
    }

}
