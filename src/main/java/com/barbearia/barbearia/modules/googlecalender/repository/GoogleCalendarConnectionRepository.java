package com.barbearia.barbearia.modules.googlecalender.repository;

import com.barbearia.barbearia.modules.googlecalender.model.GoogleCalendarConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, Long> {

    Optional<GoogleCalendarConnection> findByUserIdAndBusinessId(Long userId, Long businessId);

    Optional<GoogleCalendarConnection> findByUserIdAndBusinessIdAndActiveTrue(Long userId, Long businessId);
}

