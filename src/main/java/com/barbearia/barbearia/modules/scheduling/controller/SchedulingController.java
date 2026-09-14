package com.barbearia.barbearia.modules.scheduling.controller;

import com.barbearia.barbearia.modules.catalog.dto.request.AddServiceRequest;
import com.barbearia.barbearia.modules.scheduling.dto.request.EndSchedulingRequest;
import com.barbearia.barbearia.modules.scheduling.dto.request.ReasonRequest;
import com.barbearia.barbearia.modules.scheduling.dto.request.SchedulingRequest;
import com.barbearia.barbearia.modules.scheduling.dto.request.SchedulingStaffRequest;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingResponse;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.scheduling.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    /**
     * Lista paginada de todos os agendamentos da barbearia atual.
     */
    @GetMapping
    public ResponseEntity<Page<SchedulingResponse>> listAll(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(schedulingService.listAll(pageable));
    }

    /**
     * Agendamentos num intervalo de datas (usado pela agenda semanal/mensal).
     */
    @GetMapping("/business/{slug}/range")
    public ResponseEntity<List<SchedulingResponse>> listSchedulingsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        return ResponseEntity.ok(schedulingService.getByDateRange(startDateTime, endDateTime));
    }

    /**
     * Um agendamento específico pelo id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SchedulingResponse> schedulingId(@PathVariable Long id) {
        return ResponseEntity.ok(schedulingService.getById(id));
    }

    /**
     * Agendamento do client autenticado.
     */
    @GetMapping("/{me}")
    public ResponseEntity<List<SchedulingResponse>> listMine(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(schedulingService.getByClientId(userDetails.id()));
    }

    /**
     * Agenda do barbeiro autenticado dentro da barbearia atual.
     */
    @GetMapping("/per-barber")
    public List<SchedulingResponse> listByBarber(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return schedulingService.getByBarberId(userDetails.id());
    }

    /**
     * Agendamentos em um único dia.
     */
    @GetMapping("/per-day")
    public ResponseEntity<List<SchedulingResponse>> listByDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(LocalTime.MAX);

        return ResponseEntity.ok(schedulingService.getByDateTime(startDate, endDate));
    }

    /**
     * Retorna os horários disponíveis de um barbeiro para uma data e conjunto
     * de serviços. É consumido pela tela pública de agendamento antes da criação
     * da reserva.
     * O formato da data é definido explicitamente para evitar dependência de
     * configurações globais do Spring Boot.
     */
    @GetMapping("/available-times")
    public ResponseEntity<List<LocalTime>> getAvailableTimes(
            @RequestParam LocalDate date,
            @RequestParam List<Long> barberServiceIds,
            @RequestParam Long barberId) {

        return ResponseEntity.ok(schedulingService.getAvailableSlots(date, barberServiceIds, barberId));
    }

    /**
     * Criar um agendamento para si mesmo.
     */
    @PostMapping
    public ResponseEntity<SchedulingResponse> create(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid SchedulingRequest schedulingRequest) {

        SchedulingResponse created = schedulingService.createScheduling(userDetails.id(), schedulingRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Recepção/barbeiro agendando para um cliente que não tem conta
     * (nome e telefone digitados na hora).
     */
    @PostMapping("/staff")
    public ResponseEntity<SchedulingResponse> createByStaff(@RequestBody @Valid SchedulingStaffRequest request) {

        SchedulingResponse created = schedulingService.createStaffScheduling(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);

    }

    /**
     * Cliente cancelando o próprio agendamento.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelByClient(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("id") Long schedulingId) {
        schedulingService.cancelClient(userDetails.id(), schedulingId);
        return ResponseEntity.noContent().build();
    }


    /**
     * Barbeiro, gerente ou dono cancelando, com motivo registrado.
     */
    @PostMapping("/barber/{id}")
    public ResponseEntity<Void> cancelByStaff(
            @PathVariable Long schedulingId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody ReasonRequest reasonRequest) {
        
        schedulingService.cancelByBusinessMember(schedulingId, userDetails.id(), reasonRequest);

        return ResponseEntity.noContent().build();
    }

    /**
     * Inicia o atendimento e abre a comanda.
     */
    @PatchMapping("/{id}/start")
    public ResponseEntity<Void> startScheduling(
            @PathVariable("id") Long schedulingId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        schedulingService.startScheduling(schedulingId, userDetails.id());
        return ResponseEntity.ok().build();
    }

    /**
     * Finaliza o atendimento: forma de pagamento, serviços extras, produtos usados
     * (com baixa de estoque) e valores adicionais por barbeiro.
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<SchedulingResponse> complete(
            @PathVariable("id") Long schedulingId,
            @Valid @RequestBody EndSchedulingRequest request, 
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        SchedulingResponse response = schedulingService.endService(schedulingId, request, userDetails.id());

        return ResponseEntity.ok(response);
    }

    /**
     * Adiciona serviços a um agendamento em andamento
     * (quando o cliente pede um novo serviço apos já ter começado o atendimento).
     */
    @PostMapping("/{id}/services")
    public ResponseEntity<SchedulingResponse> addService(
            @PathVariable("id") Long schedulingId,
            @Valid @RequestBody AddServiceRequest request) {
        
        SchedulingResponse response = schedulingService.addService(schedulingId, request.barberServiceIds());

        return ResponseEntity.ok(response);
    }
}
