package com.barbearia.barbearia.modules.business.dto.response;

import java.util.List;

public record BusinessResponse(
        Long id,
        String name,
        String description,
        String slug,
        List<String> amenities,
        boolean active,
        String businessImage,
        String backgroundImage,
        String owner,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf
) {

}
