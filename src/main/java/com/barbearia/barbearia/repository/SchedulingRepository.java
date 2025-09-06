package com.barbearia.barbearia.repository;

import com.barbearia.barbearia.model.Scheduling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchedulingRepository extends JpaRepository<Scheduling, Long> {

    List<Scheduling> findByUser_Id(Long id);

    Optional<Scheduling> findByBarber_IdAndDateTime(Long barberId, LocalDateTime dateTime);

    List<Scheduling> findByBarber_Id(Long barberId);

    List<Scheduling> findByDateTimeBetween(LocalDateTime dateTimeAfter, LocalDateTime dateTimeBefore);

    List<Scheduling> findByBarber_IdAndDateTimeBetween(Long barberId, LocalDateTime start, LocalDateTime end);
}
