package com.barbearia.barbearia.modules.leads.dto.request;

public record LeadRequest(
    String name,
    String email,
    String businessName,
    String telephone,
    String plan
) {
    
}
