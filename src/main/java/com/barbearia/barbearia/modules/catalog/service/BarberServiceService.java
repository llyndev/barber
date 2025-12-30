package com.barbearia.barbearia.modules.catalog.service;

import com.barbearia.barbearia.modules.catalog.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.modules.catalog.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.catalog.mapper.BarberServiceMapper;
import com.barbearia.barbearia.modules.catalog.model.BarberService;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.catalog.repository.BarberServiceRepository;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.tenant.BusinessContext;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
@RequiredArgsConstructor
public class BarberServiceService {

    private final BarberServiceRepository barberServiceRepository;
    private final BarberServiceMapper barberServiceMapper;
    private final UserBusinessRepository userBusinessRepository;
    private final BusinessRepository businessRepository;

    private Long getBusinessIdFromContext() {
        String businessIdStr = BusinessContext.getBusinessId();
        if (businessIdStr == null || businessIdStr.isBlank()) {
            throw new IllegalStateException("Business ID not foun");
        }
        return Long.parseLong(businessIdStr);
    }

    private void checkOwnerManagerPermission() {
        String role = BusinessContext.getBusinessRole();
        if (!"OWNER".equals(role) && !"MANAGER".equals(role)) {
            throw new SecurityException("Unauthorized.");
        }
    }

    public List<BarberServiceResponse> listAll() {

        String businessIdStr = BusinessContext.getBusinessId();

        if (businessIdStr == null) {
            throw new IllegalStateException("Business not found");
        }

        Long businessId = Long.parseLong(businessIdStr);

        List<BarberService> barberService = barberServiceRepository.findAllByBusinessIdAndActiveTrue(businessId);

        return barberService.stream()
                .map(BarberServiceMapper::toDTO)
                .toList();
    }

    public BarberServiceResponse getById(Long id) {

        String businessIdStr = BusinessContext.getBusinessId();
        if (businessIdStr == null) {
            throw new IllegalStateException("Nenhum business selecionado.");
        }
        Long businessId = Long.parseLong(businessIdStr);

        return barberServiceRepository.findByIdAndBusinessId(id, businessId) // CORRETO
            .map(BarberServiceMapper::toDTO)
            .orElseThrow( () -> new ResourceNotFoundException("Service not found in this business"));
}

    public BarberService getEntityById(Long id) {
        return barberServiceRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Service not found"));
    }

    @Transactional
    public BarberService createService(BarberService service) {


        String businessIdStr = BusinessContext.getBusinessId();

        if (businessIdStr == null || businessIdStr.isBlank()) {
            throw new IllegalStateException("Problem not resolved in the request. Business ID is missing from context.");
        }

        Long businessId = Long.parseLong(businessIdStr);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new SecurityException("User not authenticated");
        }

        Long authenticatedUserId = userDetails.user().getId();

        boolean allowed = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(authenticatedUserId, businessId, BusinessRole.OWNER) || userBusinessRepository.existsByUserIdAndBusinessIdAndRole(authenticatedUserId, businessId, BusinessRole.MANAGER);

        if (!allowed) throw new SecurityException("Unauthorized");

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business not found"));

        service.setId(null);
        service.setBusiness(business);
        service.setActive(true);

        return barberServiceRepository.save(service);

    }

    public BarberServiceResponse save(BarberServiceRequest barberServiceRequest, UserDetailsImpl userDetails) {

        if (userDetails == null) {
            throw new SecurityException("User not authenticated");
        }
        Long currentUserId = userDetails.user().getId();

        String businessIdStr = BusinessContext.getBusinessId();
        if (businessIdStr == null || businessIdStr.isBlank()) {
            throw new IllegalStateException("Business ID is missing from context. ");
        }
        Long businessId = Long.parseLong(businessIdStr);

        boolean allowed = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(currentUserId, businessId, BusinessRole.OWNER)
                || userBusinessRepository.existsByUserIdAndBusinessIdAndRole(currentUserId, businessId, BusinessRole.MANAGER);
        
        if (!allowed) {
            throw new SecurityException("Unauthorized. Requer permissão de OWNER ou MANAGER para este business.");
        }

        // 3. Busca a entidade Business
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business not found com ID: " + businessId));

        // 4. Mapeia e Salva o novo serviço
        BarberService barberService = barberServiceMapper.toEntity(barberServiceRequest);
        barberService.setId(null); // Garante que é um novo
        barberService.setBusiness(business); // Vincula ao Business correto
        barberService.setActive(true);

        barberService = barberServiceRepository.save(barberService);

        return BarberServiceMapper.toDTO(barberService);
    }

    public BarberServiceResponse update(Long id, BarberServiceRequest request) {

        checkOwnerManagerPermission();
        Long businessId = getBusinessIdFromContext();

        BarberService barberService = barberServiceRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow( () -> new ResourceNotFoundException("Service not found"));

        if (request.nameService() != null) {
            barberService.setNameService(request.nameService());
        }
        if (request.description() != null) {
            barberService.setDescription(request.description());
        }
        if (request.durationInMinutes() != null) {
            barberService.setDurationInMinutes(request.durationInMinutes());
        }
        if (request.price() != null) {
            barberService.setPrice(request.price());
        }

        BarberService updatedService = barberServiceRepository.save(barberService);

        return BarberServiceMapper.toDTO(updatedService);
    }

    public void delete(Long id){

        checkOwnerManagerPermission();
        Long businessId = getBusinessIdFromContext();

        BarberService service = barberServiceRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        barberServiceRepository.delete(service);
    }

    @Transactional(readOnly = true)
    public List<BarberServiceResponse> findAllByBusinessId(Long businessId) {
        List<BarberService> barberServices = barberServiceRepository.findAllByBusinessIdAndActiveTrue(businessId);
        return barberServices.stream()
                .map(BarberServiceMapper::toDTO)
                .toList();
    }

}
