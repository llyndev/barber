package com.barbearia.barbearia.modules.business.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PlanType {
    SOLO(1, 1),
    BASIC(1, 3),
    PREMIUM(2, 6),
    ENTERPRISE(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int maxBusiness;
    private final int maxBarbers;
}

