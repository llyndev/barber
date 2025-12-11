package com.barbearia.barbearia.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.dto.request.BusinessRequest;
import com.barbearia.barbearia.dto.response.BusinessResponse;
import com.barbearia.barbearia.model.Address;
import com.barbearia.barbearia.model.Business;

@Component
public class BusinessMapper {


    public Business toRequest(BusinessRequest request) {
        Business business = new Business();

        business.setName(request.name());
        business.setDescription(request.description());
        business.setSlug(request.slug());

        return business;
    }

    public BusinessResponse toResponse(Business business) {
        if (business == null) return null;

        Address a = business.getAddress();

        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getDescription(),
                business.getSlug(),
                a != null ? a.getCep() : null,
                a != null ? a.getLogradouro() : null,
                a != null ? a.getNumero() : null,
                a != null ? a.getComplemento() : null,
                a != null ? a.getBairro() : null,
                a != null ? a.getLocalidade() : null,
                a != null ? a.getUf() : null
        );
    }

}
