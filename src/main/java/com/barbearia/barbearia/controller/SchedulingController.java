package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.request.SchedulingRequest;
import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.mapper.SchedulingMapper;
import com.barbearia.barbearia.model.Scheduling;
import com.barbearia.barbearia.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SchedulingResponse> newScheduling(@RequestBody SchedulingRequest schedulingRequest) {

        Scheduling scheduling = new Scheduling();
        scheduling.setDateTime(schedulingRequest.getDateTime());

        Scheduling schedulingCreate = schedulingService.scheduling(
                schedulingRequest.getClientId(),
                schedulingRequest.getBarberServiceId(),
                schedulingRequest.getBarberId(),
                scheduling);

        SchedulingResponse schedulingResponse = SchedulingMapper.toResponse(schedulingCreate);

        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingResponse);
    }
}
