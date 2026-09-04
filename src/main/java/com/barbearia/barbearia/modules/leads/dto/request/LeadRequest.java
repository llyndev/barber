package com.barbearia.barbearia.modules.leads.dto.request;

import com.barbearia.barbearia.modules.business.model.PlanType;

public record LeadRequest(
    String name,
    String email,
    String businessName,
    String telephone,
    PlanType plan
) {
    
}
