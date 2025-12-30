package com.barbearia.barbearia.modules.availability.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WeeklyScheduleRequest(
    @NotNull
    @Valid
    List<OpeningHoursRequest> schedule
) {}
