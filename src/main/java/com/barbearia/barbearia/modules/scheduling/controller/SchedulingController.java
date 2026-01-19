package com.barbearia.barbearia.modules.scheduling.controller;

import com.barbearia.barbearia.modules.catalog.dto.request.AddServiceRequest;
import com.barbearia.barbearia.modules.scheduling.dto.request.EndSchedulingRequest;
import com.barbearia.barbearia.modules.scheduling.dto.request.ReasonRequest;
import com.barbearia.barbearia.modules.scheduling.dto.request.SchedulingRequest;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingResponse;
import com.barbearia.barbearia.modules.scheduling.mapper.SchedulingMapper;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.business.service.BusinessService;
import com.barbearia.barbearia.modules.scheduling.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final BusinessService businessService;

    @GetMapping
    public ResponseEntity<List<SchedulingResponse>> schedulingList(
        @RequestHeader("X-Business-Slug") String businessSlug,
        @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Business business = businessService.validateOwnerOrManagerBySlug(businessSlug, userDetails.user().getId());
 
        List<SchedulingResponse> schedulings = schedulingService.findAllByBusinessId(business.getId());

        return ResponseEntity.ok(schedulings);
    }

    @GetMapping("/business")
    public ResponseEntity<List<SchedulingResponse>> listSchedulings(
            @RequestHeader("X-Business-Slug") String businessSlug,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        // Valida se o usuário autenticado é o owner ou manager da barbearia e retorna a entidade Business
        Business business = businessService.validateOwnerOrManagerBySlug(businessSlug, userDetails.user().getId());
        
        // Retorna os agendamentos da barbearia
        List<SchedulingResponse> schedulings = schedulingService.findAllByBusinessId(business.getId());
        return ResponseEntity.ok(schedulings);
    }

    @GetMapping("/business/{slug}")
    public ResponseEntity<List<SchedulingResponse>> listSchedulingsBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        Business business = businessService.validateOwnerOrManagerBySlug(slug, userDetails.user().getId());
        
        List<SchedulingResponse> schedulings = schedulingService.findAllByBusinessId(business.getId());
        return ResponseEntity.ok(schedulings);
    }

    @GetMapping("/business/{slug}/range")
    public ResponseEntity<List<SchedulingResponse>> listSchedulingsByDateRange(
            @PathVariable String slug,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Business business = businessService.validateOwnerOrManagerBySlug(slug, userDetails.user().getId());

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<SchedulingResponse> schedulings = schedulingService.getByDateRange(startDateTime, endDateTime, business.getId());
        return ResponseEntity.ok(schedulings);
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

        Long clientId = userDetails.user().getId();

        schedulingService.cancelClient(id, clientId, userDetails);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/barber/{id}")
    public ResponseEntity<Void> cancelByBusinessMember(
            @PathVariable Long id, 
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody ReasonRequest reasonRequest,
            @RequestHeader(value = "X-Business-Slug", required = false) String businessSlug) {
        
        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateBusinessMemberBySlug(businessSlug, userDetails.user().getId());
        }
        
        schedulingService.cancelByBusinessMember(id, userDetails.user().getId(), reasonRequest.reason(), userDetails);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/barber/completed/{id}")
    public ResponseEntity<SchedulingResponse> completedScheduling(
            @PathVariable Long id, 
            @Valid @RequestBody EndSchedulingRequest request, 
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "X-Business-Slug", required = false) String businessSlug) {

        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateBarberBySlug(businessSlug, userDetails.user().getId());
        }

        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateOwnerOrManagerBySlug(businessSlug, userDetails.user().getId());
        }

        Long barberId = userDetails.user().getId();

        Scheduling scheduling = schedulingService.endService(id, request, barberId);

        SchedulingResponse schedulingResponse = SchedulingMapper.toResponse(scheduling);

        return ResponseEntity.ok(schedulingResponse);
    }

    @PostMapping("/barber/add-service/{id}")
    public ResponseEntity<SchedulingResponse> addService(
            @PathVariable Long id, 
            @Valid @RequestBody AddServiceRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "X-Business-Slug", required = false) String businessSlug) {
        
        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateOwnerOrManagerBySlug(businessSlug, userDetails.user().getId());
        }
        
        Scheduling scheduling = schedulingService.addService(id, request.barberServiceIds());

        SchedulingResponse response = SchedulingMapper.toResponse(scheduling);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<Void> startAppointment(
            @PathVariable Long id,
            @RequestHeader("X-Business-Slug") String businessSlug,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        businessService.validateOwnerOrManagerOrBarberBySlug(businessSlug, userDetails.user().getId());

        schedulingService.startAppointment(id);
        return ResponseEntity.ok().build();
    }
}
