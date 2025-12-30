package com.barbearia.barbearia.modules.availability.mapper;

import com.barbearia.barbearia.modules.availability.dto.request.SpecificDateRequest;
import com.barbearia.barbearia.modules.availability.dto.response.SpecificDateResponse;
import com.barbearia.barbearia.modules.availability.model.OpeningHours;
import org.springframework.stereotype.Component;

@Component
public class SpecificDateMapper {

    public void updateEntityFromRequest(OpeningHours entity, SpecificDateRequest request) {
        if (request == null || entity == null) {
            return;
        }

        entity.setSpecificDate(request.specificDate());
        entity.setActive(request.active());

        if (request.active()) {
            entity.setOpenTime(request.openTime());
            entity.setCloseTime(request.closeTime());
        } else {
            entity.setOpenTime(null);
            entity.setCloseTime(null);
        }
    }

    public SpecificDateResponse toResponse(OpeningHours openingHours) {
        if (openingHours == null) {
            return null;
        }

        return new SpecificDateResponse(
                openingHours.getId(),
                openingHours.getSpecificDate(),
                openingHours.getTypeRule(),
                openingHours.isActive(),
                openingHours.getOpenTime(),
                openingHours.getCloseTime()
        );
    }

    public OpeningHours toEntity(SpecificDateRequest request) {
        if (request == null) {
            return null;
        }

        OpeningHours entity = new OpeningHours();
        entity.setSpecificDate(request.specificDate());
        entity.setActive(request.active());
        entity.setOpenTime(request.openTime());
        entity.setCloseTime(request.closeTime());

        return entity;
    }
}
