package com.barbearia.barbearia.modules.availability.dto.response;

import java.time.LocalTime;

public record BusinessStatusResponse(
    boolean isOpen,
    String message,
    LocalTime openTime,
    LocalTime closeTime
) {}
