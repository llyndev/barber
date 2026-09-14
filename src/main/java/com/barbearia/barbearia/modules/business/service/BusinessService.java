package com.barbearia.barbearia.modules.business.service;

import java.util.List;
import java.io.IOException;

import com.barbearia.barbearia.common.util.TextNormalizer;
import com.barbearia.barbearia.exception.ConflictException;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.modules.account.dto.response.BusinessPublicResponse;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.modules.account.service.FileStorageService;
import com.barbearia.barbearia.modules.business.dto.response.BusinessSummaryResponse;
import com.barbearia.barbearia.modules.business.model.BusinessImageType;
import com.barbearia.barbearia.tenant.BusinessContext;
import com.barbearia.barbearia.tenant.BusinessGuard;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.barbearia.barbearia.modules.business.dto.request.BusinessRequest;
import com.barbearia.barbearia.modules.business.dto.response.BusinessResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.common.address.mapper.AddressMapper;
import com.barbearia.barbearia.modules.business.mapper.BusinessMapper;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.business.model.UserBusiness;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMapper businessMapper;
    private final AddressMapper addressMapper;
    private final UserBusinessRepository userBusinessRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final PlanPolicy planPolicy;
    private final SlugGenerator slugGenerator;
    private final BusinessGuard businessGuard;
    private final TransactionTemplate transactionTemplate;

    // Metodo para listar todas as barbearias
    // METODO NÃO ESTA SENDO UTILIZADO
    public List<BusinessResponse> getAll(boolean includeInactive) {
        return businessRepository.findAll().stream()
                .filter(business -> includeInactive || business.isActive())
                .map(businessMapper::toResponse)
                .toList();
    }

    // Metodo para buscar barbearias por nome, cidade e bairro
    // TODO: Adicionar permissão para pegar localização do usuário e recomendar barbearias da sua cidade.
    public Page<BusinessSummaryResponse> searchBusinesses(String searchQuery, boolean includeInactive, Pageable pageable) {
        if (searchQuery == null || searchQuery.isBlank()) {
            Page<Business> page = includeInactive
                    ? businessRepository.findAll(pageable)
                    : businessRepository.findAllByActiveTrue(pageable);
            return page.map(businessMapper::toSummary);
        }

        String query = TextNormalizer.escapeLikeWildcards(searchQuery.toLowerCase().trim());

        if (query.length() < 2) {
            throw new InvalidRequestException("Enter at least 2 characters to search.");
        }

        return businessRepository.search(query, includeInactive, pageable)
                .map(businessMapper::toSummary);
    }

    // Metodo que da sugestões para autocomplete no search
    public List<BusinessSummaryResponse> suggest(String searchQuery, int limit) {
        Pageable topResults = PageRequest.of(0, Math.min(limit, 10));

        return businessRepository.search(TextNormalizer.escapeLikeWildcards(searchQuery), false, topResults)
                .map(businessMapper::toSummary)
                .getContent(); // extrai a List de dentro da Page
    }

    public BusinessResponse getById(Long id) {
        return businessRepository.findById(id)
                .map(businessMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found"));
    }

    public BusinessPublicResponse getPublicBySlug(String slug) {
        return businessRepository.findBySlugAndActiveTrue(slug)
                .map(businessMapper::toPublicResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found"));
    }

    @Transactional
    public BusinessResponse create(BusinessRequest request, Long creatorId) {
        AppUser creator = userRepository.findById(creatorId).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));

        planPolicy.ensureCanCreateBusiness(creator);

        Business business = businessMapper.toEntity(request);

        business.setSlug(slugGenerator.generateUnique(request.slug()));
        business.setAddress(addressMapper.toEntity(request.address()));
        business.setOwner(creator);

        Business saved;

        try {

            saved = businessRepository.save(business);

            UserBusiness ownerLink = UserBusiness.builder()
                    .user(creator)
                    .business(saved)
                    .role(BusinessRole.OWNER)
                    .build();
            userBusinessRepository.save(ownerLink);

            businessRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("This address is already in use. Choose another name.");
        }

        return businessMapper.toResponse(saved);
    }

    @Transactional // TODO: ARRUMAR ESSE METODO URGENTE!
    public BusinessResponse update(BusinessRequest request) {
        businessGuard.requireOwner();

        Business business = businessRepository.findById(BusinessContext.requireBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found"));

        business.setAddress(addressMapper.toEntity(request.address()));

        business.setName(request.name());
        business.setDescription(request.description());
        business.setTelephone(request.telephone());
        business.setAmenities(request.amenities());
        business.setInstagramLink(request.instagramLink());

        return businessMapper.toResponse(business);
    }

    @Transactional
    public BusinessResponse activate(Long businessId, Long userId) {
        UserBusiness membership = userBusinessRepository
                .findByUserIdAndBusinessId(userId, businessId)
                .orElseThrow(() -> new SecurityException("Unauthorized."));

        if (membership.getRole() != BusinessRole.OWNER) {
            throw new SecurityException("Unauthorized.");
        }

        Business business = membership.getBusiness();
        business.setActive(true);

        return businessMapper.toResponse(business);
    }

    public List<BusinessSummaryResponse> findAllByOwnerId(Long ownerId) {
        return userBusinessRepository.findAllByUserIdAndRole(ownerId, BusinessRole.OWNER)
                .stream()
                .map(userBusiness -> userBusiness.getBusiness())
                .map(businessMapper::toSummary)
                .toList();
    }

    @Transactional
    public BusinessResponse deactivate() {
        businessGuard.requireOwner();

        Business business = businessRepository.findById(BusinessContext.requireBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        business.setActive(false);
        Business response = businessRepository.save(business);

        return businessMapper.toResponse(response);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateBusinessImage(BusinessImageType type, MultipartFile file) throws IOException {
        businessGuard.requireOwner();
        Long businessId = BusinessContext.requireBusinessId();

        String newFileName = fileStorageService.saveImage(file, "business/" + businessId);

        String oldFileName;
        try {
            oldFileName = transactionTemplate.execute(
                    status -> swapImageName(businessId, type, newFileName));

        } catch (RuntimeException ex) {
            fileStorageService.deleteImage(newFileName);
            throw ex;
        }

        if (oldFileName != null) {
            fileStorageService.deleteImage(oldFileName);
        }
        return newFileName;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void removeBusinessImage(BusinessImageType type) {
        businessGuard.requireOwner();
        Long businessId = BusinessContext.requireBusinessId();

        String oldFileName = transactionTemplate.execute(
                status -> swapImageName(businessId, type, null));

        if (oldFileName != null) {
            fileStorageService.deleteImage(oldFileName);
        }
    }

    protected String swapImageName(Long businessId, BusinessImageType type, String newFileName) {
            Business business = businessRepository.findById(businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Barber not found."));

            return switch (type) {
                case LOGO -> {
                    String previous = business.getBusinessImage();
                    business.setBusinessImage(newFileName);
                    yield previous;
                }
                case BACKGROUND -> {
                    String previous = business.getBackgroundImage();
                    business.setBackgroundImage(newFileName);
                    yield previous;
                }
            };
    }
}
