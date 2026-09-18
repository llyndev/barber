package com.barbearia.barbearia.modules.business.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateSlotIntervalRequest(
        @NotNull Integer slotIntervalMinutes
) {}
