package com.barbearia.barbearia.modules.scheduling.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record CreateSchedulingStaffRequest(
        Long barberId,
        Long clientId,
        List<Long> serviceIds,
        LocalDateTime start,
        boolean force
) {
}
