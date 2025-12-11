package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.EndSchedulingRequest;
import com.barbearia.barbearia.dto.request.SchedulingRequest;
import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.exception.ConflictingScheduleException;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.SchedulingMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.model.BarberService;
import com.barbearia.barbearia.model.Business;
import com.barbearia.barbearia.model.BusinessRole;
import com.barbearia.barbearia.model.Scheduling;
import com.barbearia.barbearia.model.AppointmentStatus;
import com.barbearia.barbearia.repository.BarberServiceRepository;
import com.barbearia.barbearia.repository.BusinessRepository;
import com.barbearia.barbearia.repository.SchedulingRepository;
import com.barbearia.barbearia.repository.UserBusinessRepository;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.tenant.BusinessContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private static final int SLOT_MINUTES = 15;

    private final UserService userService;
    private final SchedulingRepository schedulingRepository;
    private final BarberServiceRepository barberServiceRepository;
    private final OpeningHoursService openingHoursService;
    private final BusinessRepository businessRepository;
    private final UserBusinessRepository userBusinessRepository;

    private Long getBusinessIdFromContext() {
        String businessIdStr = BusinessContext.getBusinessId();
        if (businessIdStr == null || businessIdStr.isBlank()) {
            throw new IllegalStateException("Business ID not found.");
        }

        return Long.parseLong(businessIdStr);
    }

    private Business getBusinessEntityFromContext() {
        Long businessId = getBusinessIdFromContext();
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not foun"));
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponse> listAll() {
        Long businessId = getBusinessIdFromContext();
        List<Scheduling> scheduling = schedulingRepository.findAllByBusinessId(businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponse> findAllByBusinessId(Long businessId) {
        List<Scheduling> scheduling = schedulingRepository.findAllByBusinessId(businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    @Transactional(readOnly = true)
    public SchedulingResponse getById(Long id) {
        Long businessId = getBusinessIdFromContext();
        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(id, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Scheduling not found"));
        return SchedulingMapper.toResponse(scheduling);
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponse> getByClientId(Long id) {
        Long businessId = getBusinessIdFromContext();
        List<Scheduling> scheduling = schedulingRepository.findByUser_IdAndBusinessId(id, businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    public List<SchedulingResponse> getByBarberId(Long id) {
        Long businessId = getBusinessIdFromContext();
        List<Scheduling> scheduling = schedulingRepository.findByBarber_IdAndBusinessId(id, businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    public List<SchedulingResponse> getByDateTime(LocalDateTime start, LocalDateTime end) {
        Long businessId = getBusinessIdFromContext();
        List<Scheduling> scheduling = schedulingRepository.findByDateTimeBetweenAndBusinessId(start, end, businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    @Transactional
    public Scheduling createScheduling(Long clientId, SchedulingRequest request) {
        log.info("Creating scheduling for client {} with barber {} at {}", 
                clientId, request.barberId(), request.dateTime());

        Long businessId = getBusinessIdFromContext();
        Business business = getBusinessEntityFromContext();

        AppUser user = userService.getEntityById(clientId);
        AppUser barber = userService.getEntityById(request.barberId());

        boolean isBarberInThisBusiness = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barber.getId(), businessId, BusinessRole.BARBER);

        if (!isBarberInThisBusiness) {
            throw new ResourceNotFoundException("Barber invalid");
        }


        List<BarberService> barberService = barberServiceRepository.findAllById(request.barberServiceIds());

        List<BarberService> validServices = barberService.stream()
                .filter(service -> service.getBusiness().getId().equals(businessId))
                .toList();

        if (barberService.isEmpty() || validServices.size() != request.barberServiceIds().size()) {
            throw new ResourceNotFoundException("Barber service not found");
        }

        LocalDateTime start = request.dateTime().withSecond(0).withNano(0);
        ensureAvailableOrThrow(barber.getId(), validServices, start, businessId);

        Scheduling sched = new Scheduling();
        sched.setUser(user);
        sched.setBarber(barber);
        sched.setBarberService(validServices);
        sched.setDateTime(request.dateTime());
        sched.setStates(AppointmentStatus.SCHEDULED);
        sched.setBusiness(business);

        Scheduling saved = schedulingRepository.save(sched);
        log.info("Scheduling created successfully with ID: {}", saved.getId());

        return saved;

    }

    @Transactional
    public void cancelClient(Long schedulingId, Long clientId, @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long businessId = getBusinessIdFromContext();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId)
                .orElseThrow( () -> new ResourceNotFoundException("Scheduling not found"));

        if (userDetails == null) {
            throw new RuntimeException("User not found");
        }

        if (!scheduling.getUser().getId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "FORBIDDEN");
        }

        scheduling.setStates(AppointmentStatus.CANCELED);
        schedulingRepository.save(scheduling);
    }

    @Transactional
    public void cancelBarber(Long schedulingId, Long barberId, String reason, @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long businessId = getBusinessIdFromContext();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Scheduling not found")
        );

        boolean isManagerOrOwner = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.OWNER) || userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.MANAGER);

        boolean isAssignedBarber = scheduling.getBarber().getId().equals(barberId);

        if (!isManagerOrOwner && !isAssignedBarber) {
            throw new SecurityException("Unauthorized");
        }

        scheduling.setStates(AppointmentStatus.CANCELED);
        scheduling.setReasonCancel(reason);
        schedulingRepository.save(scheduling);
    }

    public Scheduling endService(Long schedulingId, EndSchedulingRequest endSchedulingRequest, Long barberId) {
        Long businessId = getBusinessIdFromContext();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Scheduling not found")
        );

        boolean isManagerOrOwner = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.OWNER) || userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.MANAGER);

        boolean isAssignedBarber = scheduling.getBarber().getId().equals(barberId);

        if (!isManagerOrOwner && !isAssignedBarber) {
            throw new SecurityException("Unauthorized");
        }
        
        if (scheduling.getStates() != AppointmentStatus.SCHEDULED) {
            throw new InvalidRequestException("Bad request");
        }

        scheduling.setStates(AppointmentStatus.COMPLETED);
        scheduling.setObservation(endSchedulingRequest.observation());
        scheduling.setAdditionalValue(endSchedulingRequest.additionalValue());
        scheduling.setPaymentMethod(endSchedulingRequest.paymentMethod());

        return schedulingRepository.save(scheduling);
    }

    @Transactional
    public Scheduling addService(Long schedulingId, List<Long> newServiceIds) {

        Long businessId = getBusinessIdFromContext();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Scheduling not found")
        );

        List<BarberService> newService = barberServiceRepository.findAllById(newServiceIds);
        List<BarberService> validServices = newService.stream()
                .filter(services -> services.getBusiness().getId().equals(businessId))
                .toList();

        if (newService.isEmpty() || validServices.size() != newServiceIds.size()) {
            throw new ResourceNotFoundException("Not found");
        }

        for (BarberService serviceToAdd : newService) {
            if (!scheduling.getBarberService().contains(serviceToAdd)) {
                scheduling.getBarberService().add(serviceToAdd);
            }
        }

        return schedulingRepository.save(scheduling);

    }

    public List<LocalTime> getAvailableSlots(LocalDate date, List<Long> barberServiceIds, Long barberId) {

        Long businessId = getBusinessIdFromContext();

        if (barberServiceIds == null || barberServiceIds.isEmpty()) {
            return List.of();
        }

        List<BarberService> barberServices = barberServiceRepository.findAllById(barberServiceIds);
        List<BarberService> validServices = barberServices.stream()
                .filter(services -> services.getBusiness().getId().equals(businessId))
                .toList();

        if (validServices.size() != barberServiceIds.size()) {
            throw new ResourceNotFoundException("One or more services not found.");
        }

        int totalDurationInMinutes = validServices.stream()
                .mapToInt(s -> s.getDurationInMinutes() != null ? s.getDurationInMinutes() : 0)
                .sum();

        final Duration durationService = Duration.ofMinutes(totalDurationInMinutes);

        final long slotsNeeded = (long) Math.ceil((double) durationService.toMinutes() / SLOT_MINUTES);

        var hoursOpt = openingHoursService.findForDate(date);
        if (hoursOpt.isEmpty() || !hoursOpt.get().active()) {
            return List.of();
        }

        final LocalTime open = hoursOpt.get().openTime();
        final LocalTime close = hoursOpt.get().closeTime();

        List<LocalTime> daySlots = new ArrayList<>();
        LocalTime t = open;
        while (!t.isAfter(close.minusMinutes(SLOT_MINUTES))) {
            daySlots.add(t);
            t = t.plusMinutes(SLOT_MINUTES);
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Scheduling> appointments = schedulingRepository.findByBarber_IdAndDateTimeBetweenAndBusinessId(barberId, startOfDay, endOfDay, businessId);

        Set<LocalTime> occupied = new HashSet<>();

        for (Scheduling scheduling : appointments) {
            if (scheduling == null) continue;
            if (scheduling.getDateTime() == null) continue;
            if (scheduling.getStates() != AppointmentStatus.SCHEDULED) continue;

            LocalTime start = scheduling.getDateTime().toLocalTime();

            int existingDurationInMinutes = scheduling.getBarberService().stream()
                    .mapToInt(BarberService::getDurationInMinutes)
                    .sum();

            long schedulingSlots = (long) Math.ceil((double) existingDurationInMinutes / SLOT_MINUTES);

            LocalTime st = start;
            for (int i = 0; i < schedulingSlots; i++) {
                if (!st.isBefore(close)) break;
                occupied.add(st);
                st = st.plusMinutes(SLOT_MINUTES);
            }
        }

        LocalTime lastPossibleStartTime = close.minus(durationService);
        if (lastPossibleStartTime.isBefore(open)) {
            return List.of();
        }

        boolean isToday = date.isEqual(LocalDate.now());
        LocalTime nowTime = LocalTime.now();

        return daySlots.stream()
                .filter(s -> !s.isAfter(lastPossibleStartTime))
                .filter(s -> !occupied.contains(s))
                .filter(s -> !isToday || s.isAfter(nowTime))
                .filter(s -> {
                    for (int i = 1; i < slotsNeeded; i++) {
                        LocalTime next = s.plusMinutes(i * SLOT_MINUTES);
                        if (next.isAfter(close)) return false;
                        if (occupied.contains(next)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void ensureAvailableOrThrow(Long barberId, List<BarberService> barberService, LocalDateTime start, Long businessId) {
        if (start == null) {
            throw new InvalidRequestException("Appointment details and time are mandatory.");
        }

        if ((start.getMinute() % SLOT_MINUTES) != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new InvalidRequestException("Hour not found.");
        }

        if (start.toLocalDate().isEqual(LocalDate.now()) && !start.toLocalTime().isAfter(LocalTime.now())) {
            throw new InvalidRequestException("Bad request");
        }

        var hoursOpt = openingHoursService.findForDate(start.toLocalDate());
        if (hoursOpt.isEmpty() || !hoursOpt.get().active()) {
            throw new ConflictingScheduleException("Barber closed");
        }

        LocalTime open = hoursOpt.get().openTime();
        LocalTime close = hoursOpt.get().closeTime();

        int totalDurationInMinutes = barberService.stream()
                .mapToInt(s -> s.getDurationInMinutes() != null ? s.getDurationInMinutes() : 0)
                .sum();

        Duration totalDuration = Duration.ofMinutes(totalDurationInMinutes);

        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = startTime.plus(totalDuration);

        if (startTime.isBefore(open) || endTime.isAfter(close)) {
            throw new InvalidRequestException("Appointment is outside of opening hours.");
        }

        LocalDateTime dayStart = start.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = start.toLocalDate().atTime(LocalTime.MAX);

        List<Scheduling> dayAppointments = schedulingRepository
                .findByBarber_IdAndDateTimeBetweenAndBusinessId(barberId, dayStart, dayEnd, businessId);

        LocalDateTime newEnd = start.plus(totalDuration);

        for (Scheduling scheduling : dayAppointments) {
            if (scheduling == null || scheduling.getDateTime() == null) continue;
            if (scheduling.getStates() != AppointmentStatus.SCHEDULED) continue;

            int existingDurationInMinutes = scheduling.getBarberService().stream()
                    .mapToInt(BarberService::getDurationInMinutes)
                    .sum();

            LocalDateTime existingStart = scheduling.getDateTime();
            LocalDateTime existingEnd = scheduling.getDateTime()
                    .plusMinutes(existingDurationInMinutes);

            boolean overlaps = start.isBefore(existingEnd) && newEnd.isAfter(existingStart);
            if (overlaps) {
                throw new ConflictingScheduleException("Hour conflicting");
            }
        }
    }
}
