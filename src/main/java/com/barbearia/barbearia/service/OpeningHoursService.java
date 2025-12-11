package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.OpeningHoursRequest;
import com.barbearia.barbearia.dto.request.SpecificDateRequest;
import com.barbearia.barbearia.dto.response.OpeningHoursResponse;
import com.barbearia.barbearia.dto.response.SpecificDateResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.OpeningHoursMapper;
import com.barbearia.barbearia.mapper.SpecificDateMapper;
import com.barbearia.barbearia.model.Business;
import com.barbearia.barbearia.model.OpeningHours;
import com.barbearia.barbearia.model.OpeningHours.TypeRule;
import com.barbearia.barbearia.repository.BusinessRepository;
import com.barbearia.barbearia.repository.OpeningHoursRepository;
import com.barbearia.barbearia.repository.UserBusinessRepository;
import com.barbearia.barbearia.tenant.BusinessContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        return openingHoursRepository.findAllByBusinessId(businessId)
                .stream()
                .map(openingHoursMapper::toResponse)
                .toList();
    }

    public List<OpeningHoursResponse> findWeeklySchedule() {
        Long businessId = getBusinessIdFromContext();
        return openingHoursRepository.findAllByTypeRuleAndBusinessId(TypeRule.RECURRING, businessId)
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
                    findByTypeRuleAndDayOfWeekAndBusinessId(TypeRule.RECURRING, dto.dayOfWeek(), businessId);

            OpeningHours entity;
            if (existingRuleOpt.isPresent()) {
                entity = existingRuleOpt.get();
                openingHoursMapper.updateEntityFromRequest(entity, dto);
            } else {
                entity = openingHoursMapper.toEntity(dto);
            }

            entity.setTypeRule(TypeRule.RECURRING);
            entity.setBusiness(business);
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
                findByTypeRuleAndSpecificDateAndBusinessId(TypeRule.SPECIFIC_DATE, date, businessId)
                .or(() -> openingHoursRepository.findByTypeRuleAndDayOfWeekAndBusinessId(TypeRule.RECURRING, date.getDayOfWeek(), businessId))
                .map(openingHoursMapper::toResponse);
    }

    public List<SpecificDateResponse> findSpecificDate() {
        Long businessId = getBusinessIdFromContext();
        return openingHoursRepository.findAllByTypeRuleAndBusinessId(TypeRule.SPECIFIC_DATE, businessId)
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


}
