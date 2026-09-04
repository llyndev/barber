package com.barbearia.barbearia.modules.business.dto.response;

public record BusinessSummaryResponse(
        Long id,
        String name,
        String slug,
        String imageUrl,
        String bairro,
        String numero,
        String localidade,
        String uf
) {}
