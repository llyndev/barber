package com.barbearia.barbearia.modules.account.dto.response;

import com.barbearia.barbearia.modules.common.address.dto.response.AddressResponseDTO;

import java.util.List;

public record BusinessPublicResponse(
        Long id,
        String name,
        String description,
        String telephone,
        String slug,
        List<String> amenities,
        boolean active,
        String instagramLink,
        String businessImage,
        String backgroundImage,
        AddressResponseDTO addres
) {
}
