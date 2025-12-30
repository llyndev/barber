package com.barbearia.barbearia.modules.availability.mapper;

import com.barbearia.barbearia.modules.availability.dto.request.OpeningHoursRequest;
import com.barbearia.barbearia.modules.availability.dto.response.OpeningHoursResponse;
import com.barbearia.barbearia.modules.availability.model.OpeningHours;
import org.springframework.stereotype.Component;

@Component
public class OpeningHoursMapper {

    public void updateEntityFromRequest(OpeningHours entity, OpeningHoursRequest request) {
        if (request == null || entity == null) {
            return;
        }

        entity.setDayOfWeek(request.dayOfWeek());
        entity.setActive(request.active());

        if (request.active()) {
            entity.setOpenTime(request.openTime());
            entity.setCloseTime(request.closeTime());
        } else {
            entity.setOpenTime(null);
            entity.setCloseTime(null);
        }
    }

    public OpeningHoursResponse toResponse(OpeningHours openingHours) {
        if (openingHours == null) {
            return null;
        }

        return new OpeningHoursResponse(
                openingHours.getId(),
                openingHours.getTypeRule(),
                openingHours.getDayOfWeek(),
                openingHours.isActive(),
                openingHours.getOpenTime(),
                openingHours.getCloseTime()
        );
    }

    public OpeningHours toEntity(OpeningHoursRequest request) {
        if (request == null) {
            return null;
        }

        OpeningHours entity = new OpeningHours();
        entity.setDayOfWeek(request.dayOfWeek());
        entity.setActive(request.active());
        entity.setOpenTime(request.openTime());
        entity.setCloseTime(request.closeTime());

        return entity;
    }

}
