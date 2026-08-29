package com.barbearia.barbearia.modules.catalog.service;

import com.barbearia.barbearia.modules.catalog.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.modules.catalog.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.catalog.mapper.BarberServiceMapper;
import com.barbearia.barbearia.modules.catalog.model.BarberService;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.catalog.repository.BarberServiceRepository;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.tenant.BusinessContext;

import com.barbearia.barbearia.tenant.BusinessGuard;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BarberServiceService {

    private final BarberServiceRepository barberServiceRepository;
    private final BarberServiceMapper barberServiceMapper;
    private final BusinessRepository businessRepository;
    private final BusinessGuard businessGuard;

    public List<BarberServiceResponse> listAll() {

        Long businessId = BusinessContext.requireBusinessId();

        List<BarberService> barberService = barberServiceRepository.findAllByBusinessIdAndActiveTrue(businessId);

        return barberService.stream()
                .map(barberServiceMapper::toDTO)
                .toList();
    }

    public BarberServiceResponse getById(Long id) {

        Long businessId = BusinessContext.requireBusinessId();

        return barberServiceRepository.findByIdAndBusinessId(id, businessId)
            .map(barberServiceMapper::toDTO)
            .orElseThrow( () -> new ResourceNotFoundException("Service not found in this business"));
    }

    @Transactional
    public BarberServiceResponse save(BarberServiceRequest barberServiceRequest) {

        businessGuard.requireOwnerOrManager();

        Long businessId = BusinessContext.requireBusinessId();

        Business business = businessRepository.getReferenceById(businessId);

        // 4. Mapeia e Salva o novo serviço
        BarberService barberService = barberServiceMapper.toEntity(barberServiceRequest);
        barberService.setId(null);
        barberService.setBusiness(business); // Vincula ao Business correto
        barberService.setActive(true);

        barberService = barberServiceRepository.save(barberService);

        return barberServiceMapper.toDTO(barberService);
    }

    @Transactional
    public BarberServiceResponse update(Long id, BarberServiceRequest request) {

        businessGuard.requireOwnerOrManager();

        Long businessId = BusinessContext.requireBusinessId();

        BarberService barberService = barberServiceRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow( () -> new ResourceNotFoundException("Service not found"));

        barberServiceMapper.updateEntityFromRequest(request, barberService);

        barberService = barberServiceRepository.save(barberService);

        return barberServiceMapper.toDTO(barberService);
    }

    @Transactional
    public void delete(Long id){
        businessGuard.requireOwnerOrManager();
        Long businessId = BusinessContext.requireBusinessId();

        BarberService service = barberServiceRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        barberServiceRepository.delete(service);
    }

    public List<BarberServiceResponse> findBusinessBySlug(String slug) {
        List<BarberService> barberServices = barberServiceRepository.findAllByBusinessSlugAndActiveTrue(slug);

        return barberServices.stream()
                .map(barberServiceMapper::toDTO)
                .toList();
    }

}
