package com.barbearia.barbearia.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.dto.response.AddressResponse;
import com.barbearia.barbearia.model.Address;

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
