package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.request.SchedulingRequest;
import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.mapper.SchedulingMapper;
import com.barbearia.barbearia.model.Scheduling;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/scheduling")
public class SchedulingController {

    private final SchedulingService schedulingService;

    @GetMapping
    public List<SchedulingResponse> schedulingList() {
        return schedulingService.getAll();
    }

    @PostMapping
    public ResponseEntity<SchedulingResponse> newScheduling(@AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody SchedulingRequest schedulingRequest) {

        Long clientId = userDetails.user().getId();

        Scheduling newScheduling = schedulingService.createScheduling(clientId, schedulingRequest);

        SchedulingResponse schedulingResponse = SchedulingMapper.toResponse(newScheduling);
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingResponse);
    }

    @DeleteMapping("{id}/canceled")
    public ResponseEntity<Void> cancelClient(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long clientId = userDetails.user().getId();
        schedulingService.cancelClient(id, clientId);
        return ResponseEntity.noContent().build();

    }
}
