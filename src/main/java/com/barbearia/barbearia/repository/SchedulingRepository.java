package com.barbearia.barbearia.repository;

import com.barbearia.barbearia.model.Scheduling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SchedulingRepository extends JpaRepository<Scheduling, Long> {

    Optional<Scheduling> findByBarber_IdAndDateTime(Long barberId, LocalDateTime dateTime);
}
