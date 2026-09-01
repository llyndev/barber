package com.barbearia.barbearia.modules.common.address.mapper;

import com.barbearia.barbearia.modules.common.address.dto.request.AddressRequest;
import com.barbearia.barbearia.modules.common.address.dto.response.AddressResponseDTO;
import org.springframework.stereotype.Component;

import com.barbearia.barbearia.modules.common.address.dto.response.AddressResponse;
import com.barbearia.barbearia.modules.common.address.model.Address;

@Component
public class AddressMapper {


    public Address toEntity(AddressRequest request) {
        if (request == null) {
            return null;
        }

        return Address.of(
                request.cep(),
                request.logradouro(),
                request.complemento(),
                request.bairro(),
                request.numero(),
                request.cidade(),
                request.uf()
        );
    }

    public AddressResponseDTO toResponse(Address address) {
        if (address == null) {
            return null;
        }

        return new AddressResponseDTO(
                formatCep(address.getCep()),
                address.getLogradouro(),
                address.getNumero(),
                address.getComplemento(),
                address.getBairro(),
                address.getLocalidade(),
                address.getUf()
        );
    }

    private String formatCep(String cep) {
        if (cep == null || cep.length() != 8) {
            return cep;
        }

        return cep.substring(0, 5) + "-" + cep.substring(5);
    }
}
