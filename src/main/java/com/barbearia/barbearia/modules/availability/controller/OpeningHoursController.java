package com.barbearia.barbearia.modules.availability.controller;

import com.barbearia.barbearia.modules.availability.dto.request.SpecificDateRequest;
import com.barbearia.barbearia.modules.availability.dto.request.OpeningHoursRequest;
import com.barbearia.barbearia.modules.availability.dto.response.OpeningHoursResponse;
import com.barbearia.barbearia.modules.availability.dto.response.SpecificDateResponse;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.business.service.BusinessService;
import com.barbearia.barbearia.modules.availability.service.OpeningHoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/opening-hours")
@RequiredArgsConstructor
public class OpeningHoursController {

    private final OpeningHoursService openingHoursService;
    private final BusinessService businessService;

    @GetMapping
    public List<OpeningHoursResponse> listAll(@RequestHeader("X-Business-Slug") String businessSlug) {
        return openingHoursService.listAll();
    }

    @GetMapping("/weekly-schedule")
    public ResponseEntity<List<OpeningHoursResponse>> findWeeklySchedule(@RequestHeader("X-Business-Slug") String businessSlug) {
        List<OpeningHoursResponse> weeklySchedule = openingHoursService.findWeeklySchedule();
        return ResponseEntity.ok(weeklySchedule);
    }

    @PutMapping("/weekly-schedule")
    public ResponseEntity<List<OpeningHoursResponse>> upsertWeeklySchedule(
            @Valid @RequestBody List<OpeningHoursRequest> request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader("X-Business-Slug") String businessSlug) {
        
        businessService.validateOwnerOrManagerBySlug(businessSlug, userDetails.user().getId());
        
        List<OpeningHoursResponse> updatedSchedule = openingHoursService.upsertWeeklySchedule(request);
        return ResponseEntity.ok(updatedSchedule);
    }

    @GetMapping("/specific-date")
    public ResponseEntity<List<SpecificDateResponse>> findSpecificDate(@RequestHeader("X-Business-Slug") String businessSlug) {
        List<SpecificDateResponse> specificDate = openingHoursService.findSpecificDate();
        return ResponseEntity.ok(specificDate);
    }

    @PostMapping("/specific-date")
    public ResponseEntity<SpecificDateResponse> createSpecificDate(
            @Valid @RequestBody SpecificDateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader("X-Business-Slug") String businessSlug) {
        
        businessService.validateOwnerOrManagerBySlug(businessSlug, userDetails.user().getId());
        
        SpecificDateResponse createdSpecificDate = openingHoursService.createSpecificDate(request);
        return ResponseEntity.ok(createdSpecificDate);
    }

    @PutMapping("/specific-date/{id}")
    public ResponseEntity<SpecificDateResponse> updateSpecificDate(
            @PathVariable Long id, 
            @Valid @RequestBody SpecificDateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader("X-Business-Slug") String businessSlug) {
        
        businessService.validateOwnerOrManagerBySlug(businessSlug, userDetails.user().getId());
        
        SpecificDateResponse updateSpecificDate = openingHoursService.updateSpecificDate(id, request);
        return ResponseEntity.ok(updateSpecificDate);
    }

    @DeleteMapping("/specific-date/{id}")
    public ResponseEntity<Void> deleteSpecificDate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader("X-Business-Slug") String businessSlug) {
        
        businessService.validateOwnerOrManagerBySlug(businessSlug, userDetails.user().getId());
        
        openingHoursService.deleteSpecificDate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/barber/{barberId}/weekly-schedule")
    public ResponseEntity<List<OpeningHoursResponse>> findBarberWeeklySchedule(
            @PathVariable Long barberId,
            @RequestHeader("X-Business-Slug") String businessSlug) {
        List<OpeningHoursResponse> weeklySchedule = openingHoursService.findBarberWeeklySchedule(barberId);
        return ResponseEntity.ok(weeklySchedule);
    }

    @PutMapping("/barber/{barberId}/weekly-schedule")
    public ResponseEntity<List<OpeningHoursResponse>> upsertBarberWeeklySchedule(
            @PathVariable Long barberId,
            @Valid @RequestBody List<OpeningHoursRequest> request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader("X-Business-Slug") String businessSlug) {
        
        businessService.validateBusinessMemberBySlug(businessSlug, userDetails.user().getId());
        
        List<OpeningHoursResponse> updatedSchedule = openingHoursService.upsertBarberWeeklySchedule(barberId, request, userDetails.user());
        return ResponseEntity.ok(updatedSchedule);
    }

}
