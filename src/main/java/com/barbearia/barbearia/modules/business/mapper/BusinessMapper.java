package com.barbearia.barbearia.modules.business.mapper;

import com.barbearia.barbearia.modules.account.model.AppUser;
import org.springframework.stereotype.Component;

import com.barbearia.barbearia.modules.business.dto.request.BusinessRequest;
import com.barbearia.barbearia.modules.business.dto.response.BusinessResponse;
import com.barbearia.barbearia.modules.common.address.model.Address;
import com.barbearia.barbearia.modules.business.model.Business;

@Component
public class BusinessMapper {

    private static final String BUSINESS_URL = "/uploads/";

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

        AppUser owner = business.getOwner();

        String businessUrl = null;
        if (business.getBusinessImage() != null && !business.getBusinessImage().isBlank()) {
            businessUrl = BUSINESS_URL + business.getBusinessImage();
        }

        String backgroundUrl = null;
        if (business.getBackgroundImage() != null && !business.getBackgroundImage().isBlank()) {
            backgroundUrl = BUSINESS_URL + business.getBackgroundImage();
        }

        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getDescription(),
                business.getTelephone(),
                business.getSlug(),
                business.getAmenities(),
                business.isActive(),
                business.getInstagramLink(),
                businessUrl,
                backgroundUrl,
                owner.getName(),
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
