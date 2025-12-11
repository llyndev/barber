package com.barbearia.barbearia.service;

import java.util.List;

import com.barbearia.barbearia.model.AppUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import com.barbearia.barbearia.dto.request.BusinessRequest;
import com.barbearia.barbearia.dto.response.BusinessResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.AddressMapper;
import com.barbearia.barbearia.mapper.BusinessMapper;
import com.barbearia.barbearia.model.Address;
import com.barbearia.barbearia.model.Business;
import com.barbearia.barbearia.model.BusinessRole;
import com.barbearia.barbearia.model.UserBusiness;
import com.barbearia.barbearia.repository.BusinessRepository;
import com.barbearia.barbearia.repository.UserBusinessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMapper businessMapper;
    private final AddressService addressService;
    private final AddressMapper addressMapper;
    private final UserBusinessRepository userBusinessRepository;

    public List<BusinessResponse> getAll() {
        return businessRepository.findAll().stream()
                .filter(business -> business.isActive())
                .map(businessMapper::toResponse)
                .toList();
    }

    public List<BusinessResponse> searchBusinesses(String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return getAll();
        }

        String query = searchQuery.toLowerCase().trim();
        
        return businessRepository.findAll().stream()
                .filter(business -> business.isActive())
                .filter(business -> {
                    // Busca por nome
                    if (business.getName() != null && business.getName().toLowerCase().contains(query)) {
                        return true;
                    }
                    // Busca por endereço
                    if (business.getAddress() != null) {
                        Address addr = business.getAddress();
                        if (addr.getLogradouro() != null && addr.getLogradouro().toLowerCase().contains(query)) {
                            return true;
                        }
                        if (addr.getBairro() != null && addr.getBairro().toLowerCase().contains(query)) {
                            return true;
                        }
                        if (addr.getLocalidade() != null && addr.getLocalidade().toLowerCase().contains(query)) {
                            return true;
                        }
                        if (addr.getUf() != null && addr.getUf().toLowerCase().contains(query)) {
                            return true;
                        }
                        if (addr.getCep() != null && addr.getCep().contains(query)) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(businessMapper::toResponse)
                .toList();
    }

    public List<BusinessResponse> findAllByOwnerId(Long ownerId) {
        return userBusinessRepository.findAllByUserIdAndRole(ownerId, BusinessRole.OWNER).stream().map(userBusiness -> userBusiness.getBusiness()).map(businessMapper::toResponse).toList();
    }


    public BusinessResponse getById(Long id) {
        Business business =  businessRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Business not found"));

        return businessMapper.toResponse(business);
    }

    public Business getBusinessBySlug(String slug) {
        return businessRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado para o slug: " + slug));
    }

    public BusinessResponse getBySlug(String slug) {
        Business business = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado para o slug: " + slug));
        return businessMapper.toResponse(business);
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

        Business business = businessMapper.toRequest(request);

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

    /**
     * Valida se o usuário autenticado é o owner da barbearia do slug fornecido.
     * @param businessSlug O slug da barbearia
     * @param authenticatedUserId O ID do usuário autenticado
     * @throws ResourceNotFoundException Se a barbearia não for encontrada
     * @throws SecurityException Se o usuário não for o owner da barbearia
     */
    public void validateOwnerBySlug(String businessSlug, Long authenticatedUserId) {
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
    }

    /**
     * Valida se o usuário autenticado é o owner da barbearia do slug fornecido e retorna a entidade Business.
     * @param businessSlug O slug da barbearia
     * @param authenticatedUserId O ID do usuário autenticado
     * @return A entidade Business se a validação passar
     * @throws ResourceNotFoundException Se a barbearia não for encontrada
     * @throws SecurityException Se o usuário não for o owner da barbearia
     */
    public Business validateOwnerBySlugAndGetBusiness(String businessSlug, Long authenticatedUserId) {
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

}
