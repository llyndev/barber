package com.barbearia.barbearia.modules.common.address.dto.response;

public record AddressResponseDTO(
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String localidade,
        String uf
) {
}
