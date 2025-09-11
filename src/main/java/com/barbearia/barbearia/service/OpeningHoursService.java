package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.OpeningHoursRequest;
import com.barbearia.barbearia.dto.request.SpecificDateRequest;
import com.barbearia.barbearia.dto.response.OpeningHoursResponse;
import com.barbearia.barbearia.dto.response.SpecificDateResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.OpeningHoursMapper;
import com.barbearia.barbearia.mapper.SpecificDateMapper;
import com.barbearia.barbearia.model.OpeningHours;
import com.barbearia.barbearia.model.OpeningHours.TypeRule;
import com.barbearia.barbearia.repository.OpeningHoursRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    public List<OpeningHoursResponse> listAll() {
        return openingHoursRepository.findAll()
                .stream()
                .map(openingHoursMapper::toResponse)
                .toList();
    }

    public List<OpeningHoursResponse> findWeeklySchedule() {
        return openingHoursRepository.findAllByTypeRule(TypeRule.RECURRING)
                .stream()
                .map(openingHoursMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<OpeningHoursResponse> upsertWeeklySchedule(List<OpeningHoursRequest> request) {
        if (request.size() != 7) {
            throw new IllegalArgumentException("The weekly schedule must contain exactly 7 days.");
        }

        List<OpeningHours> rulesToSave = request.stream().map(dto -> {

            Optional<OpeningHours> existingRuleOpt = openingHoursRepository.
                    findByTypeRuleAndDayOfWeek(TypeRule.RECURRING, dto.dayOfWeek());

            OpeningHours entity;
            if (existingRuleOpt.isPresent()) {
                entity = existingRuleOpt.get();
                openingHoursMapper.updateEntityFromRequest(entity, dto);
            } else {
                entity = openingHoursMapper.toEntity(dto);
            }

            entity.setTypeRule(TypeRule.RECURRING);
            return entity;
        }).collect(Collectors.toList());

        List<OpeningHours> savedRules = openingHoursRepository.saveAll(rulesToSave);

        return savedRules.stream()
                .map(openingHoursMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<OpeningHoursResponse> findForDate(LocalDate date) {
        return openingHoursRepository.
                findByTypeRuleAndSpecificDate(TypeRule.SPECIFIC_DATE, date)
                .or(() -> openingHoursRepository.findByTypeRuleAndDayOfWeek(TypeRule.RECURRING, date.getDayOfWeek()))
                .map(openingHoursMapper::toResponse);
    }

    public List<SpecificDateResponse> findSpecificDate() {
        return openingHoursRepository.findAllByTypeRule(TypeRule.SPECIFIC_DATE)
                .stream()
                .map(specificDateMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SpecificDateResponse createSpecificDate(SpecificDateRequest request) {
        if (request.specificDate() == null) {
            throw new IllegalArgumentException("A specific date is required for an exception rule.");
        }

        OpeningHours newSpecificDate = specificDateMapper.toEntity(request);
        newSpecificDate.setTypeRule(TypeRule.SPECIFIC_DATE);

        OpeningHours saveSpecificDate = openingHoursRepository.save(newSpecificDate);
        return specificDateMapper.toResponse(saveSpecificDate);
    }

    @Transactional
    public SpecificDateResponse updateSpecificDate(Long id, SpecificDateRequest request) {
        OpeningHours existingSpecificDate = openingHoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exception rule with id " + id + " not found."));

        specificDateMapper.updateEntityFromRequest(existingSpecificDate, request);

        OpeningHours updatedSpecificDate = openingHoursRepository.save(existingSpecificDate);
        return specificDateMapper.toResponse(updatedSpecificDate);

    }

    @Transactional
    public void deleteSpecificDate(Long id) {
        if (!openingHoursRepository.existsById(id)) {
            throw new ResourceNotFoundException("Exception rule with id " + id + " not found.");
        }
        openingHoursRepository.deleteById(id);
    }


}
