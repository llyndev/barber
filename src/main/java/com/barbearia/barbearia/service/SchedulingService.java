package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.SchedulingRequest;
import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.exception.ConflictingScheduleException;
import com.barbearia.barbearia.mapper.SchedulingMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.model.BarberService;
import com.barbearia.barbearia.model.Scheduling;
import com.barbearia.barbearia.model.AppointmentStatus;
import com.barbearia.barbearia.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final UserService userService;
    private final BarberServiceService barberServiceService;
    private final SchedulingRepository schedulingRepository;

    public List<SchedulingResponse> getAll() {
        List<Scheduling> appointments = schedulingRepository.findAll();
        return SchedulingMapper.toResponseList(appointments);
    }

    @Transactional
    public Scheduling createScheduling(Long clientId, SchedulingRequest request) {
        AppUser user = userService.getEntityById(clientId);
        AppUser barber = userService.getEntityById(request.barberId());
        BarberService barberService = barberServiceService.getEntityById(request.barberServiceId());

        Optional<Scheduling> conflicts = schedulingRepository.findByBarber_IdAndDateTime(request.barberId(), request.dateTime());
        if (conflicts.isPresent()) {
            throw new ConflictingScheduleException("This barber already has an appointment for the selected time.");
        }

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
                .orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheduling not found"));

        if (!scheduling.getId().equals(clientId)) {
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
}
