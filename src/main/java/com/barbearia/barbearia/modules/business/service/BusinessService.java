package com.barbearia.barbearia.modules.business.service;

import java.util.List;
import java.io.IOException;

import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.common.address.service.AddressService;
import com.barbearia.barbearia.modules.account.service.FileStorageService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.barbearia.barbearia.modules.business.dto.request.BusinessRequest;
import com.barbearia.barbearia.modules.business.dto.response.BusinessResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.common.address.mapper.AddressMapper;
import com.barbearia.barbearia.modules.business.mapper.BusinessMapper;
import com.barbearia.barbearia.modules.common.address.model.Address;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.business.model.PlanType;
import com.barbearia.barbearia.modules.business.model.UserBusiness;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMapper businessMapper;
    private final AddressService addressService;
    private final AddressMapper addressMapper;
    private final UserBusinessRepository userBusinessRepository;
    private final FileStorageService fileStorageService;

    public List<BusinessResponse> getAll(boolean includeInactive) {
        return businessRepository.findAll().stream()
                .filter(business -> includeInactive || business.isActive())
                .map(businessMapper::toResponse)
                .toList();
    }

    public List<BusinessResponse> searchBusinesses(String searchQuery, boolean includeInactive) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return getAll(includeInactive);
        }

        String query = searchQuery.toLowerCase().trim();

        return businessRepository.findAll().stream()
                .filter(business -> includeInactive || business.isActive())
                .filter(business -> 
                    business.getName().toLowerCase().contains(query) ||
                    (business.getAddress() != null && (
                        (business.getAddress().getLocalidade() != null && business.getAddress().getLocalidade().toLowerCase().contains(query)) ||
                        (business.getAddress().getBairro() != null && business.getAddress().getBairro().toLowerCase().contains(query))
                    ))
                )
                .map(businessMapper::toResponse)
                .toList();
    }

    public BusinessResponse getById(Long id) {
        return businessRepository.findById(id)
                .map(businessMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
    }

    public BusinessResponse getBySlug(String slug) {
        return businessRepository.findBySlug(slug)
                .map(businessMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
    }

    public Business getBusinessBySlug(String slug) {
        return businessRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado para o slug: " + slug));
    }

    @Transactional
    public BusinessResponse create(BusinessRequest request, AppUser creator) {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Business name is required");
        }

        // Valida se o usuário é BUSINESS_OWNER
        if (creator.getPlatformRole() != AppUser.PlatformRole.BUSINESS_OWNER) {
            throw new SecurityException("Apenas usuários com role BUSINESS_OWNER podem criar barbearias. Entre em contato com o suporte para contratar um plano.");
        }

        // Valida se o usuário tem plano ativo
        boolean hasActivePlan = creator.getDateExpirationAccount() != null
                && creator.getDateExpirationAccount().isAfter(java.time.LocalDate.now());

        if (!hasActivePlan) {
            throw new SecurityException("Você precisa ter um plano ativo para criar uma barbearia. Entre em contato com o suporte.");
        }

        // Valida limite de barbearias do plano
        PlanType userPlan;
        try {
            userPlan = PlanType.valueOf(creator.getPlantType());
        } catch (Exception e) {
            throw new IllegalStateException("Usuário sem tipo de plano definido.");
        }

        long ownedBusinesses = userBusinessRepository.countByUserIdAndRole(creator.getId(), BusinessRole.OWNER);
        if (ownedBusinesses >= userPlan.getMaxBusiness()) {
            throw new IllegalStateException("Seu plano " + userPlan.name() + " permite apenas " + userPlan.getMaxBusiness() + " barbearia(s). Faça upgrade do plano para criar mais.");
        }

        Business business = businessMapper.toRequest(request);
        business.setOwner(creator);

        // gerar slug simples se não informado
        if (business.getSlug() == null || business.getSlug().isBlank()) {
            business.setSlug(business.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        }

        businessRepository.findBySlug(business.getSlug()).ifPresent(b -> {
            throw new IllegalArgumentException("Slug in use");
        });

        // se cep informado, buscar endereço pelo AddressService e preencher número/complemento
        if (request.cep() != null && !request.cep().isBlank()) {
            var addrResp = addressService.getCep(request.cep());
            if (addrResp != null) {
                Address addr = addressMapper.toEntity(addrResp);
                addr.setNumero(request.numero());
                addr.setComplemento(request.complemento());
                business.setAddress(addr);
            }
        }

        // Define a data de expiração do plano baseada na conta do usuário
        if (creator.getDateExpirationAccount() != null) {
            business.setPlanExpirationDate(creator.getDateExpirationAccount().atTime(23, 59, 59));
        }

        Business saved = businessRepository.save(business);

        UserBusiness ownerLink = UserBusiness.builder()
                .user(creator)
                .business(saved)
                .role(BusinessRole.OWNER)
                .build();
        userBusinessRepository.save(ownerLink);

        return businessMapper.toResponse(saved);
    }

    public Business validateOwnerBySlug(String businessSlug, Long authenticatedUserId) {
        if (businessSlug == null || businessSlug.isBlank()) {
            throw new IllegalArgumentException("Business slug é obrigatório");
        }

        Business business = businessRepository.findBySlug(businessSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado para o slug: " + businessSlug));

        boolean isOwner = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(
                authenticatedUserId,
                business.getId(),
                BusinessRole.OWNER
        );

        if (!isOwner) {
            throw new SecurityException("Acesso negado. Apenas o owner da barbearia pode realizar esta operação.");
        }

        return business;
    }

    public Business validateBusinessMemberBySlug(String businessSlug, Long authenticatedUserId) {
        if (businessSlug == null || businessSlug.isBlank()) {
            throw new IllegalAccessError("Acesso negado");
        }

        Business business = businessRepository.findBySlug(businessSlug)
            .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado"));

        boolean hasAcess = userBusinessRepository.existsByUserIdAndBusinessIdAndRoleIn(authenticatedUserId, business.getId(), List.of(BusinessRole.OWNER, BusinessRole.MANAGER, BusinessRole.BARBER));

        if (!hasAcess) {
            throw new SecurityException("Acesso negado");
        }

        return business;
    }

    public Business validateOwnerOrManagerBySlug(String businessSlug, Long authenticatedUserId) {
        if (businessSlug == null || businessSlug.isBlank()) {
            throw new IllegalAccessError("Acesso negado");
        }

        Business business = businessRepository.findBySlug(businessSlug)
            .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado"));

        boolean hasAcess = userBusinessRepository.existsByUserIdAndBusinessIdAndRoleIn(authenticatedUserId, business.getId(), List.of(BusinessRole.OWNER, BusinessRole.MANAGER));

        if (!hasAcess) {
            throw new SecurityException("Acesso negado");
        }

        return business;
    }

    public Business validateOwnerOrManagerOrBarberBySlug(String businessSlug, Long authenticatedUserId) {
        if (businessSlug == null || businessSlug.isBlank()) {
            throw new IllegalAccessError("Acesso negado");
        }

        Business business = businessRepository.findBySlug(businessSlug)
            .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado"));

        boolean hasAcess = userBusinessRepository.existsByUserIdAndBusinessIdAndRoleIn(authenticatedUserId, business.getId(), List.of(BusinessRole.OWNER, BusinessRole.MANAGER, BusinessRole.BARBER));

        if (!hasAcess) {
            throw new SecurityException("Acesso negado");
        }

        return business;
    }

    public Business validateBarberBySlug(String businessSlug, Long authenticatedUserId) {
        if (businessSlug == null || businessSlug.isBlank()) {
            throw new IllegalArgumentException("Business slug é obrigatório");
        }

        Business business = businessRepository.findBySlug(businessSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado para o slug: " + businessSlug));

        boolean isBarber = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(
                authenticatedUserId,
                business.getId(),
                BusinessRole.BARBER
        );

        if (!isBarber) {
            throw new SecurityException("Acesso negado. Apenas o barbeiro pode realizar esta operação.");
        }

        return business;
    }

    @Transactional
    public BusinessResponse update(Long id, BusinessRequest request, AppUser user) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        boolean isOwner = business.getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getPlatformRole() == AppUser.PlatformRole.PLATFORM_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("Only owner or admin can update business");
        }

        if (request.cep() != null && !request.cep().isBlank()) {
            var addrResp = addressService.getCep(request.cep());
            if (addrResp != null) {
                Address address = addressMapper.toEntity(addrResp);
                address.setNumero(request.numero());
                address.setComplemento(request.complemento());
                business.setAddress(address);
            }
        }

        business.setName(request.name());
        business.setDescription(request.description());
        business.setTelephone(request.telephone());
        business.setAmenities(request.amenities());
        business.setInstagramLink(request.instagramLink());

        return businessMapper.toResponse(businessRepository.save(business));
    }

    @Transactional
    public BusinessResponse activateBusiness(String slug, AppUser user) {
        Business business = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        if (user.getPlatformRole() != AppUser.PlatformRole.PLATFORM_ADMIN) {
            throw new SecurityException("Only admin can activate business");
        }

        business.setActive(true);
        Business response = businessRepository.save(business);

        return businessMapper.toResponse(response);
    }

    public List<BusinessResponse> findAllByOwnerId(Long ownerId) {
        return userBusinessRepository.findAllByUserIdAndRole(ownerId, BusinessRole.OWNER).stream().map(userBusiness -> userBusiness.getBusiness()).map(businessMapper::toResponse).toList();
    }

    @Transactional
    public BusinessResponse deactivateBusiness(String slug, AppUser user) {
        Business business = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        boolean isOwner = business.getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getPlatformRole() == AppUser.PlatformRole.PLATFORM_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new SecurityException("Only owner or admin can deactivate business");
        }

        business.setActive(false);
        Business response = businessRepository.save(business);

        return businessMapper.toResponse(response);
    }

    public List<BusinessResponse> getMyBusinesses(AppUser user) {
        return businessRepository.findByOwnerId(user.getId()).stream()
                .map(businessMapper::toResponse)
                .toList();
    }

    @Transactional
    public String updateBusinessImage(Long businessId, Long userId, String type, MultipartFile file) throws IOException {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        if (!business.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied");
        }

        String oldImage = null;
        if ("LOGO".equalsIgnoreCase(type)) {
            oldImage = business.getBusinessImage();
        } else if ("BACKGROUND".equalsIgnoreCase(type)) {
            oldImage = business.getBackgroundImage();
        } else {
            throw new IllegalArgumentException("Invalid image type. Use LOGO or BACKGROUND");
        }

        String folder = "business/" + businessId;
        String fileName = fileStorageService.saveImage(file, folder);

        if (oldImage != null) {
            fileStorageService.deleteImage(oldImage);
        }

        if ("LOGO".equalsIgnoreCase(type)) {
            business.setBusinessImage(fileName);
        } else {
            business.setBackgroundImage(fileName);
        }
        
        businessRepository.save(business);
        return fileName;
    }

    @Transactional
    public void removeBusinessImage(Long businessId, Long userId, String type) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        if (!business.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied");
        }

        String imageToRemove = null;
        if ("LOGO".equalsIgnoreCase(type)) {
            imageToRemove = business.getBusinessImage();
            business.setBusinessImage(null);
        } else if ("BACKGROUND".equalsIgnoreCase(type)) {
            imageToRemove = business.getBackgroundImage();
            business.setBackgroundImage(null);
        } else {
            throw new IllegalArgumentException("Invalid image type. Use LOGO or BACKGROUND");
        }

        if (imageToRemove != null) {
            fileStorageService.deleteImage(imageToRemove);
        }
        
        businessRepository.save(business);
    }
}
