package com.barbearia.barbearia.modules.business.service;

import java.util.List;
import java.io.IOException;

import com.barbearia.barbearia.exception.ConflictException;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.model.PlatformRole;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.modules.business.dto.response.BusinessSummaryResponse;
import com.barbearia.barbearia.modules.common.address.service.AddressService;
import com.barbearia.barbearia.modules.account.service.FileStorageService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.barbearia.barbearia.modules.business.dto.request.BusinessRequest;
import com.barbearia.barbearia.modules.business.dto.response.BusinessResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.common.address.mapper.AddressMapper;
import com.barbearia.barbearia.modules.business.mapper.BusinessMapper;
import com.barbearia.barbearia.modules.common.address.model.Address;
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
    private final AddressService addressService;
    private final AddressMapper addressMapper;
    private final UserBusinessRepository userBusinessRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final PlanPolicy planPolicy;
    private final SlugGenerator slugGenerator;

    // Metodo para listar todas as barbearias
    public List<BusinessResponse> getAll(boolean includeInactive) {
        return businessRepository.findAll().stream()
                .filter(business -> includeInactive || business.isActive())
                .map(businessMapper::toResponse)
                .toList();
    }

    // Metodo para buscar barbearia por
    public Page<BusinessSummaryResponse> searchBusinesses(String searchQuery, boolean includeInactive, Pageable pageable) {
        if (searchQuery == null || searchQuery.isBlank()) {
            Page<Business> page = includeInactive
                    ? businessRepository.findAll(pageable)
                    : businessRepository.findAllByActiveTrue(pageable);
            return page.map(businessMapper::toSummary);
        }

        String query = searchQuery.toLowerCase().trim();

        if (query.length() < 2) {
            throw new InvalidRequestException("Enter at least 2 characters to search.");
        }

        query = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

        return businessRepository.search(query, includeInactive, pageable)
                .map(businessMapper::toSummary);
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

    @Transactional // @Valid no controller
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
