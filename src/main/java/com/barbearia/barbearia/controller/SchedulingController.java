package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.request.AddServiceRequest;
import com.barbearia.barbearia.dto.request.EndSchedulingRequest;
import com.barbearia.barbearia.dto.request.ReasonRequest;
import com.barbearia.barbearia.dto.request.SchedulingRequest;
import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.mapper.SchedulingMapper;
import com.barbearia.barbearia.model.Scheduling;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/scheduling")
public class SchedulingController {

    private final SchedulingService schedulingService;

    @GetMapping
    public List<SchedulingResponse> schedulingList() {
        return schedulingService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SchedulingResponse> schedulingId(@PathVariable Long id) {
        return ResponseEntity.ok(schedulingService.getById(id));
    }

    @GetMapping("/per-customer")
    public List<SchedulingResponse> schedulingGetByClientId(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return schedulingService.getByClientId(userDetails.user().getId());
    }

    @GetMapping("/per-barber")
    public List<SchedulingResponse> schedulingGetByBarberId(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return schedulingService.getByBarberId(userDetails.user().getId());
    }

    @GetMapping("/per-day")
    public List<SchedulingResponse> schedulingGetByDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(LocalTime.MAX);

        return schedulingService.getByDateTime(startDate, endDate);
    }

    @GetMapping("/available-times")
    public List<LocalTime> getAvailableTimes(
            @RequestParam LocalDate date,
            @RequestParam List<Long> barberServiceIds,
            @RequestParam Long barberId) {
        return schedulingService.getAvailableSlots(date, barberServiceIds, barberId);
    }

    @PostMapping
    public ResponseEntity<SchedulingResponse> newScheduling(@AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody SchedulingRequest schedulingRequest) {

        Long clientId = userDetails.user().getId();

        Scheduling newScheduling = schedulingService.createScheduling(clientId, schedulingRequest);

        SchedulingResponse schedulingResponse = SchedulingMapper.toResponse(newScheduling);
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelClient(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long clientId = userDetails.user().getId();

        schedulingService.cancelClient(id, clientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/barber/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> cancelBarber(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody ReasonRequest reasonRequest) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        schedulingService.cancelBarber(id, userDetails.user().getId(), reasonRequest.reason());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/barber/completed/{id}")
    public ResponseEntity<SchedulingResponse> completedScheduling(@PathVariable Long id, @RequestBody EndSchedulingRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long barberId = userDetails.user().getId();

        Scheduling scheduling = schedulingService.endService(id, request, barberId);

        SchedulingResponse schedulingResponse = SchedulingMapper.toResponse(scheduling);

        return ResponseEntity.ok(schedulingResponse);
    }

    @PostMapping("/barber/add-service/{id}")
    public ResponseEntity<SchedulingResponse> addService(@PathVariable Long id, @RequestBody AddServiceRequest request) {
        Scheduling scheduling = schedulingService.addService(id, request.barberServiceIds());

        SchedulingResponse response = SchedulingMapper.toResponse(scheduling);

        return ResponseEntity.ok(response);
    }
}
