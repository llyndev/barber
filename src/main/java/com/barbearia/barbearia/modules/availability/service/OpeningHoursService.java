package com.barbearia.barbearia.modules.availability.service;

import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.availability.dto.request.OpeningHoursRequest;
import com.barbearia.barbearia.modules.availability.dto.request.SpecificDateRequest;
import com.barbearia.barbearia.modules.availability.dto.response.OpeningHoursResponse;
import com.barbearia.barbearia.modules.availability.dto.response.SpecificDateResponse;
import com.barbearia.barbearia.modules.availability.dto.response.BusinessStatusResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.availability.mapper.OpeningHoursMapper;
import com.barbearia.barbearia.modules.availability.mapper.SpecificDateMapper;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.availability.model.OpeningHours;
import com.barbearia.barbearia.modules.availability.model.OpeningHours.TypeRule;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.availability.repository.OpeningHoursRepository;
import com.barbearia.barbearia.tenant.BusinessContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpeningHoursService {

    private final OpeningHoursRepository openingHoursRepository;
    private final OpeningHoursMapper openingHoursMapper;
    private final SpecificDateMapper specificDateMapper;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final UserBusinessRepository userBusinessRepository;

    private Long getBusinessIdFromContext() {
        String businessIdStr = BusinessContext.getBusinessId();
        if (businessIdStr == null || businessIdStr.isBlank()) {
            throw new IllegalStateException("Business ID not found");
        }
        return Long.parseLong(businessIdStr);
    }

    private void checkOwnerManagerPermission() {
        String role = BusinessContext.getBusinessRole();
        if (!"OWNER".equals(role) && !"MANAGER".equals(role)) {
            throw new SecurityException("Unauthorized");
        }
    }

    public List<OpeningHoursResponse> listAll() {
        Long businessId = getBusinessIdFromContext();
        return openingHoursRepository.findAllByBusinessIdAndBarberIsNull(businessId)
                .stream()
                .map(openingHoursMapper::toResponse)
                .toList();
    }

    public List<OpeningHoursResponse> findWeeklySchedule() {
        Long businessId = getBusinessIdFromContext();
        return openingHoursRepository.findAllByTypeRuleAndBusinessIdAndBarberIsNull(TypeRule.RECURRING, businessId)
                .stream()
                .map(openingHoursMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<OpeningHoursResponse> upsertWeeklySchedule(List<OpeningHoursRequest> request) {
        checkOwnerManagerPermission();
        Long businessId = getBusinessIdFromContext();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        if (request.size() != 7) {
            throw new IllegalArgumentException("The weekly schedule must contain exactly 7 days.");
        }

        List<OpeningHours> rulesToSave = request.stream().map(dto -> {
            Optional<OpeningHours> existingRuleOpt = openingHoursRepository.
                    findByTypeRuleAndDayOfWeekAndBusinessIdAndBarberIsNull(TypeRule.RECURRING, dto.dayOfWeek(), businessId);

            OpeningHours entity;
            if (existingRuleOpt.isPresent()) {
                entity = existingRuleOpt.get();
                openingHoursMapper.updateEntityFromRequest(entity, dto);
            } else {
                entity = openingHoursMapper.toEntity(dto);
            }

            entity.setTypeRule(TypeRule.RECURRING);
            entity.setBusiness(business);
            entity.setBarber(null); // Ensure it's for business
            return entity;
        }).collect(Collectors.toList());

        List<OpeningHours> savedRules = openingHoursRepository.saveAll(rulesToSave);

        return savedRules.stream()
                .map(openingHoursMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<OpeningHoursResponse> findForDate(LocalDate date) {

        Long businessId = getBusinessIdFromContext();

        return openingHoursRepository.
                findByTypeRuleAndSpecificDateAndBusinessIdAndBarberIsNull(TypeRule.SPECIFIC_DATE, date, businessId)
                .or(() -> openingHoursRepository.findByTypeRuleAndDayOfWeekAndBusinessIdAndBarberIsNull(TypeRule.RECURRING, date.getDayOfWeek(), businessId))
                .map(openingHoursMapper::toResponse);
    }

    public BusinessStatusResponse getBusinessStatus() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Optional<OpeningHoursResponse> todayHours = findForDate(today);

        if (todayHours.isEmpty() || !todayHours.get().active()) {
            return new BusinessStatusResponse(false, "Closed today", null, null);
        }

        LocalTime open = todayHours.get().openTime();
        LocalTime close = todayHours.get().closeTime();

        if (now.isBefore(open)) {
            return new BusinessStatusResponse(false, "Closed. Opens at " + open, open, close);
        } else if (now.isAfter(close)) {
            return new BusinessStatusResponse(false, "Closed. Closed at " + close, open, close);
        } else {
            return new BusinessStatusResponse(true, "Open until " + close, open, close);
        }
    }

    public List<SpecificDateResponse> findSpecificDate() {
        Long businessId = getBusinessIdFromContext();
        return openingHoursRepository.findAllByTypeRuleAndBusinessIdAndBarberIsNull(TypeRule.SPECIFIC_DATE, businessId)
                .stream()
                .map(specificDateMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SpecificDateResponse createSpecificDate(SpecificDateRequest request) {
        checkOwnerManagerPermission();
        Long businessId = getBusinessIdFromContext();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found")); 

        if (request.specificDate() == null) {
            throw new IllegalArgumentException("A specific date is required for an exception rule.");
        }

        OpeningHours newSpecificDate = specificDateMapper.toEntity(request);
        newSpecificDate.setTypeRule(TypeRule.SPECIFIC_DATE);
        newSpecificDate.setBusiness(business);

        OpeningHours saveSpecificDate = openingHoursRepository.save(newSpecificDate);
        return specificDateMapper.toResponse(saveSpecificDate);
    }

    @Transactional
    public SpecificDateResponse updateSpecificDate(Long id, SpecificDateRequest request) {
        checkOwnerManagerPermission();
        Long businessId = getBusinessIdFromContext();

        OpeningHours existingSpecificDate = openingHoursRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Exception rule with id " + id + " not found."));

        specificDateMapper.updateEntityFromRequest(existingSpecificDate, request);

        OpeningHours updatedSpecificDate = openingHoursRepository.save(existingSpecificDate);
        return specificDateMapper.toResponse(updatedSpecificDate);

    }

    @Transactional
    public void deleteSpecificDate(Long id) {
        checkOwnerManagerPermission();
        Long businessId = getBusinessIdFromContext();


        OpeningHours rule = openingHoursRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found business"));
            
        openingHoursRepository.delete(rule);
    }

    // Barber Specific Methods

    public List<OpeningHoursResponse> findBarberWeeklySchedule(Long barberId) {
        Long businessId = getBusinessIdFromContext();
        return openingHoursRepository.findAllByTypeRuleAndBusinessIdAndBarberId(TypeRule.RECURRING, businessId, barberId)
                .stream()
                .map(openingHoursMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<OpeningHoursResponse> upsertBarberWeeklySchedule(Long barberId, List<OpeningHoursRequest> request, AppUser currentUser) {
        Long businessId = getBusinessIdFromContext();
        String role = BusinessContext.getBusinessRole();

        boolean isOwnerOrManager = "OWNER".equals(role) || "MANAGER".equals(role);
        boolean isSelf = currentUser.getId().equals(barberId);

        if (!isOwnerOrManager && !isSelf) {
            throw new SecurityException("Unauthorized to update this schedule");
        }

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        
        AppUser barber = userRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found"));

        // Validate if user is actually a barber in this business
        boolean isBarber = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.BARBER) ||
                           userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.OWNER) ||
                           userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.MANAGER);
        
        if (!isBarber) {
             throw new ResourceNotFoundException("User is not a member of this business");
        }

        if (request.size() != 7) {
            throw new IllegalArgumentException("The weekly schedule must contain exactly 7 days.");
        }

        List<OpeningHours> rulesToSave = request.stream().map(dto -> {
            Optional<OpeningHours> existingRuleOpt = openingHoursRepository.
                    findByTypeRuleAndDayOfWeekAndBusinessIdAndBarberId(TypeRule.RECURRING, dto.dayOfWeek(), businessId, barberId);

            OpeningHours entity;
            if (existingRuleOpt.isPresent()) {
                entity = existingRuleOpt.get();
                openingHoursMapper.updateEntityFromRequest(entity, dto);
            } else {
                entity = openingHoursMapper.toEntity(dto);
            }

            entity.setTypeRule(TypeRule.RECURRING);
            entity.setBusiness(business);
            entity.setBarber(barber);
            return entity;
        }).collect(Collectors.toList());

        List<OpeningHours> savedRules = openingHoursRepository.saveAll(rulesToSave);

        return savedRules.stream()
                .map(openingHoursMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<OpeningHoursResponse> findForBarberAndDate(Long barberId, LocalDate date) {
        Long businessId = getBusinessIdFromContext();

        // 1. Get Business Hours
        Optional<OpeningHoursResponse> businessHours = findForDate(date);
        if (businessHours.isEmpty() || !businessHours.get().active()) {
            return Optional.empty(); // Business is closed
        }

        // 2. Get Barber Hours
        Optional<OpeningHours> barberRule = openingHoursRepository.
                findByTypeRuleAndSpecificDateAndBusinessIdAndBarberId(TypeRule.SPECIFIC_DATE, date, businessId, barberId)
                .or(() -> openingHoursRepository.findByTypeRuleAndDayOfWeekAndBusinessIdAndBarberId(TypeRule.RECURRING, date.getDayOfWeek(), businessId, barberId));

        if (barberRule.isEmpty()) {
            // If no specific rule for barber, assume they follow business hours? 
            // Or assume they don't work? 
            // Usually, if no rule is defined, they might follow business hours.
            // But the requirement says "inform individually".
            // Let's assume if no rule is defined, they follow business hours.
            return businessHours;
        }

        OpeningHours barberHours = barberRule.get();
        if (!barberHours.isActive()) {
            return Optional.empty(); // Barber is not working
        }

        // 3. Intersect Hours
        // Business: 08:00 - 20:00
        // Barber: 10:00 - 16:00
        // Result: 10:00 - 16:00

        // Business: 08:00 - 20:00
        // Barber: 18:00 - 22:00
        // Result: 18:00 - 20:00

        LocalTime businessOpen = businessHours.get().openTime();
        LocalTime businessClose = businessHours.get().closeTime();
        
        LocalTime barberOpen = barberHours.getOpenTime();
        LocalTime barberClose = barberHours.getCloseTime();

        if (barberOpen == null || barberClose == null) {
             return businessHours; // Fallback
        }

        LocalTime finalOpen = businessOpen.isAfter(barberOpen) ? businessOpen : barberOpen;
        LocalTime finalClose = businessClose.isBefore(barberClose) ? businessClose : barberClose;

        if (finalOpen.isAfter(finalClose)) {
            return Optional.empty(); // No overlap
        }

        OpeningHoursResponse response = new OpeningHoursResponse(
            null,
            TypeRule.SPECIFIC_DATE,
            barberHours.getDayOfWeek(),
            barberHours.isActive(),
            finalOpen,
            finalClose
        );

        return Optional.of(response);
    }


}
