package com.barbearia.barbearia.modules.business.mapper;

import com.barbearia.barbearia.common.util.TextNormalizer;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.dto.response.BusinessSummaryResponse;
import com.barbearia.barbearia.modules.common.address.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.barbearia.barbearia.modules.business.dto.request.BusinessRequest;
import com.barbearia.barbearia.modules.business.dto.response.BusinessResponse;
import com.barbearia.barbearia.modules.common.address.model.Address;
import com.barbearia.barbearia.modules.business.model.Business;

@Component
@RequiredArgsConstructor
public class BusinessMapper {

    private final AddressMapper addressMapper;

    @Value("${app.uploads.base-url:/uploads/}")
    private String uploadsBaseUrl;

    public Business toEntity(BusinessRequest request) {
        Business business = new Business();
        business.setName(TextNormalizer.collapseSpaces(request.name()));
        business.setDescription(request.description());
        business.setTelephone(request.telephone());
        business.setInstagramLink(request.instagramLink());
        business.setAmenities(request.amenities());

        return business;
    }

    public BusinessResponse toResponse(Business business) {
        if (business == null) return null;

        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getDescription(),
                business.getTelephone(),
                business.getSlug(),
                business.getAmenities(),
                business.isActive(),
                business.getInstagramLink(),
                buildImageUrl(business.getBusinessImage()),
                buildImageUrl(business.getBackgroundImage()),
                business.getOwner() != null ? business.getOwner().getName() : null,
                addressMapper.toResponse(business.getAddress())
        );
    }

    public BusinessSummaryResponse toSummary(Business business) {
        if (business == null) return null;

        Address address = business.getAddress();

        return new BusinessSummaryResponse(
                business.getId(),
                business.getName(),
                business.getSlug(),
                buildImageUrl(business.getBusinessImage()),
                address != null ? address.getBairro() : null,
                address != null ? address.getNumero() : null,
                address != null ? address.getLocalidade() : null,
                address != null ? address.getUf() : null
        );

    }

    private String buildImageUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        return uploadsBaseUrl + fileName;
    }
}
