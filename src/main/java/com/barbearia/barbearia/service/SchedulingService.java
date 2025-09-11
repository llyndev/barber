package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.SchedulingRequest;
import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.exception.ConflictingScheduleException;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.SchedulingMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.model.BarberService;
import com.barbearia.barbearia.model.Scheduling;
import com.barbearia.barbearia.model.AppointmentStatus;
import com.barbearia.barbearia.repository.BarberServiceRepository;
import com.barbearia.barbearia.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private static final int SLOT_MINUTES = 15;

    private final UserService userService;
    private final BarberServiceService barberServiceService;
    private final SchedulingRepository schedulingRepository;
    private final BarberServiceRepository barberServiceRepository;
    private final OpeningHoursService openingHoursService;
    private final SchedulingMapper schedulingMapper;

    public List<SchedulingResponse> listAll() {
        List<Scheduling> scheduling = schedulingRepository.findAll();
        return SchedulingMapper.toResponseList(scheduling);
    }

    public SchedulingResponse getById(Long id) {
        Scheduling scheduling = schedulingRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Scheduling not found"));
        return SchedulingMapper.toResponse(scheduling);
    }

    public List<SchedulingResponse> getByClientId(Long id) {
        List<Scheduling> scheduling = schedulingRepository.findByUser_Id(id);
        return SchedulingMapper.toResponseList(scheduling);
    }

    public List<SchedulingResponse> getByBarberId(Long id) {
        List<Scheduling> scheduling = schedulingRepository.findByBarber_Id(id);
        return SchedulingMapper.toResponseList(scheduling);
    }

    public List<SchedulingResponse> getByDateTime(LocalDateTime start, LocalDateTime end) {
        List<Scheduling> scheduling = schedulingRepository.findByDateTimeBetween(start, end);
        return SchedulingMapper.toResponseList(scheduling);
    }

    @Transactional
    public Scheduling createScheduling(Long clientId, SchedulingRequest request) {
        AppUser user = userService.getEntityById(clientId);
        AppUser barber = userService.getEntityById(request.barberId());
        BarberService barberService = barberServiceService.getEntityById(request.barberServiceId());

        LocalDateTime start = request.dateTime().withSecond(0).withNano(0);

        ensureAvailableOrThrow(barber.getId(), barberService, start);

        Scheduling scheduling = new Scheduling();
        scheduling.setUser(user);
        scheduling.setBarber(barber);
        scheduling.setBarberService(barberService);
        scheduling.setDateTime(request.dateTime());
        scheduling.setStates(AppointmentStatus.SCHEDULED);

        return schedulingRepository.save(scheduling);
    }

    @Transactional
    public void cancelClient(Long schedulingId, Long clientId) {
        Scheduling scheduling = schedulingRepository.findById(schedulingId)
                .orElseThrow( () -> new ResourceNotFoundException("Scheduling not found"));

        if (!scheduling.getUser().getId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "FORBIDDEN");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit = scheduling.getDateTime().minusHours(24);

        if (now.isAfter(limit)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        scheduling.setStates(AppointmentStatus.CANCELED);
    }

    @Transactional
    public void cancelBarber(Long schedulingId, Long barberId, String reason) {
        Scheduling scheduling = schedulingRepository.findById(schedulingId).orElseThrow(
                () -> new ResourceNotFoundException("Scheduling not found")
        );

        if (!scheduling.getBarber().getId().equals(barberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "FORBIDDEN");
        }

        final int SLOT_MINUTES = 15;
        final Duration durationService = Duration.ofMinutes(barberService.getDurationInMinutes());
        final long slotsNeeded = (long) Math.ceil((double) durationService.toMinutes() / SLOT_MINUTES);

        var hoursOpt = openingHoursService.findForDate(date);
        if (hoursOpt.isEmpty() || !hoursOpt.get().active()) {
            return List.of();
        }

        final LocalTime open = hoursOpt.get().openTime();
        final LocalTime close = hoursOpt.get().closeTime();

        List<LocalTime> daySlots = new ArrayList<>();
        for (LocalTime t = open; t.isBefore(close); t = t.plusMinutes(SLOT_MINUTES)) {
            daySlots.add(t);
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<Scheduling> appointments = schedulingRepository.findByBarber_IdAndDateTimeBetween(barberId, startOfDay, endOfDay);

        Set<LocalTime> occupied = new HashSet<>();

        for (Scheduling scheduling : appointments) {
            if (scheduling == null) continue;
            if (scheduling.getDateTime() == null) continue;
            if (scheduling.getStates() != AppointmentStatus.SCHEDULED) continue;

            LocalTime start = scheduling.getDateTime().toLocalTime();
            long schedulingSlots = (long) Math.ceil((double) scheduling.getBarberService().getDurationInMinutes() / SLOT_MINUTES);

            LocalTime t = start;
            for (int i = 0; i < schedulingSlots; i++) {
                if (!t.isBefore(close)) break;
                occupied.add(t);
                t = t.plusMinutes(SLOT_MINUTES);
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

    private void ensureAvailableOrThrow(Long barberId, BarberService barberService, LocalDateTime start) {
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

        Duration duration = Duration.ofMinutes(barberService.getDurationInMinutes());
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = startTime.plus(duration);

        if (startTime.isBefore(open) || endTime.isAfter(close) || !startTime.isBefore(close)) {
            throw new InvalidRequestException("Barber closed");
        }

        LocalDateTime dayStart = start.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = start.toLocalDate().atTime(LocalTime.MAX);
        List<Scheduling> dayAppointments = schedulingRepository
                .findByBarber_IdAndDateTimeBetween(barberId, dayStart, dayEnd);

        LocalDateTime newEnd = start.plus(duration);

        for (Scheduling scheduling : dayAppointments) {
            if (scheduling == null || scheduling.getDateTime() == null) continue;
            if (scheduling.getStates() != AppointmentStatus.SCHEDULED) continue;

            LocalDateTime existingStart = scheduling.getDateTime();
            LocalDateTime existingEnd = scheduling.getDateTime()
                    .plusMinutes(scheduling.getBarberService().getDurationInMinutes());

            boolean overlaps = start.isBefore(existingEnd) && newEnd.isAfter(existingStart);
            if (overlaps) {
                throw new ConflictingScheduleException("Hour conflicting");
            }
        }
    }
}
