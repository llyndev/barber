package com.barbearia.barbearia.modules.common.address.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.modules.common.address.dto.response.AddressResponse;
import com.barbearia.barbearia.modules.common.address.model.Address;

@Component
public class AddressMapper {


    public Address toEntity(AddressResponse response) {
        if (response == null) {
            return null;
        }

        Address address = new Address();

        address.setCep(response.cep());
        address.setLogradouro(response.logradouro());
        address.setComplemento(response.complemento());
        address.setBairro(response.bairro());
        address.setNumero(response.numero());
        address.setLocalidade(response.cidade());
        address.setUf(response.uf());

        return address;
    }
}
