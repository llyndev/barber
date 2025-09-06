package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.OpeningHoursRequest;
import com.barbearia.barbearia.dto.response.OpeningHoursResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.OpeningHoursMapper;
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
            OpeningHours entity = openingHoursRepository
                    .findByTypeRuleAndDayOfWeek(TypeRule.RECURRING, dto.dayOfWeek())
                    .orElseGet(OpeningHours::new);

            openingHoursMapper.updateEntityFromRequest(entity, dto);
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
                findByTypeRuleAndSpecificDate(TypeRule.SPECIFIC_DATA, date)
                .or(() -> openingHoursRepository.findByTypeRuleAndDayOfWeek(TypeRule.RECURRING, date.getDayOfWeek()))
                .map(openingHoursMapper::toResponse);
    }

    public List<OpeningHoursResponse> findSpecificDate() {
        return openingHoursRepository.findAllByTypeRule(TypeRule.SPECIFIC_DATA)
                .stream()
                .map(openingHoursMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OpeningHoursResponse createSpecificDate(OpeningHoursRequest request) {
        if (request.specificDate() == null) {
            throw new IllegalArgumentException("A specific date is required for an exception rule.");
        }

        OpeningHours newSpecificDate = openingHoursMapper.toEntity(request);
        newSpecificDate.setTypeRule(TypeRule.SPECIFIC_DATA);

        OpeningHours saveSpecificDate = openingHoursRepository.save(newSpecificDate);
        return openingHoursMapper.toResponse(saveSpecificDate);
    }

    @Transactional
    public OpeningHoursResponse updateSpecificDate(Long id, OpeningHoursRequest request) {
        OpeningHours existingSpecificDate = openingHoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exception rule with id " + id + " not found."));

        openingHoursMapper.updateEntityFromRequest(existingSpecificDate, request);

        OpeningHours updatedSpecificDate = openingHoursRepository.save(existingSpecificDate);
        return openingHoursMapper.toResponse(updatedSpecificDate);

    }

    @Transactional
    public void deleteSpecificDate(Long id) {
        if (!openingHoursRepository.existsById(id)) {
            throw new ResourceNotFoundException("Exception rule with id " + id + " not found.");
        }
        openingHoursRepository.deleteById(id);
    }


}
