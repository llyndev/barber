package com.barbearia.barbearia.modules.common.address.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AddressResponse(
        @JsonProperty("cep")
        String cep,

        @JsonProperty("logradouro")
        String logradouro,

        @JsonProperty("complemento")
        String complemento,

        @JsonProperty("numero")
        String numero,

        @JsonProperty("bairro")
        String bairro,

        @JsonProperty("localidade")
        String cidade,

        @JsonProperty("uf")
        String uf,

        boolean erro
) {
}