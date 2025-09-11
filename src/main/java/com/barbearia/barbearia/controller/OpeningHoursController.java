package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.request.OpeningHoursRequest;
import com.barbearia.barbearia.dto.request.SpecificDateRequest;
import com.barbearia.barbearia.dto.response.OpeningHoursResponse;
import com.barbearia.barbearia.dto.response.SpecificDateResponse;
import com.barbearia.barbearia.service.OpeningHoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/opening-hours")
@RequiredArgsConstructor
public class OpeningHoursController {

    private final OpeningHoursService openingHoursService;

    @GetMapping
    public List<OpeningHoursResponse> listAll() {
        return openingHoursService.listAll();
    }

    @GetMapping("/weekly-schedule")
    public ResponseEntity<List<OpeningHoursResponse>> findWeeklySchedule() {
        List<OpeningHoursResponse> weeklySchedule = openingHoursService.findWeeklySchedule();
        return ResponseEntity.ok(weeklySchedule);
    }

    @PutMapping("/weekly-schedule")
    public ResponseEntity<List<OpeningHoursResponse>> upsertWeeklySchedule(@RequestBody List<@Valid OpeningHoursRequest> requestList) {
        List<OpeningHoursResponse> updatedSchedule = openingHoursService.upsertWeeklySchedule(requestList);
        return ResponseEntity.ok(updatedSchedule);
    }

    @GetMapping("/specific-date")
    public ResponseEntity<List<SpecificDateResponse>> findSpecificDate() {
        List<SpecificDateResponse> specificDate = openingHoursService.findSpecificDate();
        return ResponseEntity.ok(specificDate);
    }

    @PostMapping("/specific-date")
    public ResponseEntity<SpecificDateResponse> createSpecificDate(@Valid @RequestBody SpecificDateRequest request) {
        SpecificDateResponse createdSpecificDate = openingHoursService.createSpecificDate(request);
        return ResponseEntity.ok(createdSpecificDate);
    }

    @PutMapping("/specific-date/{id}")
    public ResponseEntity<SpecificDateResponse> updateSpecificDate(@PathVariable Long id, @Valid @RequestBody SpecificDateRequest request) {
        SpecificDateResponse updateSpecificDate = openingHoursService.updateSpecificDate(id, request);
        return ResponseEntity.ok(updateSpecificDate);
    }

    @DeleteMapping("/specific-date/{id}")
    public ResponseEntity<Void> deleteSpecificDate(@PathVariable Long id) {
        openingHoursService.deleteSpecificDate(id);
        return ResponseEntity.noContent().build();
    }

}
