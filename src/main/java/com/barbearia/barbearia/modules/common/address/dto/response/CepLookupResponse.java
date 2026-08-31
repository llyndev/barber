package com.barbearia.barbearia.modules.common.address.dto.response;

public record CepLookupResponse(
        String cep,
        String logradouro,
        String bairro,
        String localidade,
        String uf
) {

    public static CepLookupResponse fromViaCep(AddressResponse viaCep) {
        return new CepLookupResponse(
                viaCep.cep().replaceAll("\\D", ""),
                viaCep.logradouro(),
                viaCep.bairro(),
                viaCep.cidade(),
                viaCep.uf()
        );
    }
}
