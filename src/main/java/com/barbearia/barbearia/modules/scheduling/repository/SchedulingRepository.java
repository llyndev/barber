package com.barbearia.barbearia.modules.scheduling.repository;

import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchedulingRepository extends JpaRepository<Scheduling, Long> {

    Optional<Scheduling> findByIdAndBusinessId(Long id, Long businessId);

    List<Scheduling> findAllByBusinessId(Long businessId);

    List<Scheduling> findByBarber_IdAndBusinessId(Long barberId, Long businessId);

    List<Scheduling> findByDateTimeBetweenAndBusinessId(LocalDateTime dateTimeAfter, LocalDateTime dateTimeBefore, Long businessId);

    List<Scheduling> findByUser_Id(Long id);

    List<Scheduling> findByBarber_IdAndDateTimeBetween(Long barberId, LocalDateTime start, LocalDateTime end);
}
