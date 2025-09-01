package com.barbearia.barbearia.dto.request;

import java.math.BigDecimal;

public record BarberServiceRequest(String nameService, String description, int durationInMinutes, BigDecimal price) {}
