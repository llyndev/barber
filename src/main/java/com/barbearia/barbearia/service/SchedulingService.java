package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.response.SchedulingResponse;
import com.barbearia.barbearia.mapper.SchedulingMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.model.BarberService;
import com.barbearia.barbearia.model.Scheduling;
import com.barbearia.barbearia.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final UserService userService;
    private final BarberServiceService barberServiceService;
    private final SchedulingRepository schedulingRepository;

    public List<SchedulingResponse> getAll() {
        List<Scheduling> schedulings = schedulingRepository.findAll();
        return SchedulingMapper.toResponseList(schedulings);
    }

    @Transactional
    public Scheduling scheduling(Long clientId, Long barberServiceId, Long barberId, Scheduling order) {
        AppUser user = userService.getEntityById(clientId);
        AppUser barber = userService.getEntityById(barberId);
        BarberService barberService = barberServiceService.getEntityById(barberServiceId);

        Optional<Scheduling> conflits = schedulingRepository.findByBarberServiceIdAndDateTime(barberServiceId, order.getDateTime());
        if (conflits.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Time already scheduled for this service");
        }

        order.setUser(user);
        order.setBarber(barber);
        order.setBarberService(barberService);

        return schedulingRepository.save(order);

    }
}
