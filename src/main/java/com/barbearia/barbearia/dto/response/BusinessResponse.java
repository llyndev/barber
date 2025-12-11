package com.barbearia.barbearia.dto.response;

public record BusinessResponse(
        Long id,
        String name,
        String description,
        String slug,
        String cep,
        String logradouro,
        String complemento,
        String numero,
        String bairro,
        String cidade,
        String uf
) {

}
