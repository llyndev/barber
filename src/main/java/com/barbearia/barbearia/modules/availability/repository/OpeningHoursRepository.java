package com.barbearia.barbearia.modules.availability.repository;

import com.barbearia.barbearia.modules.availability.model.OpeningHours;
import com.barbearia.barbearia.modules.availability.model.OpeningHours.TypeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OpeningHoursRepository extends JpaRepository<OpeningHours, Long> {

    // Business Hours (Barber is NULL)
    List<OpeningHours> findAllByBusinessIdAndBarberIsNull(Long businessId);

    List<OpeningHours> findAllByTypeRuleAndBusinessIdAndBarberIsNull(TypeRule typeRule, Long businessId);

    Optional<OpeningHours> findByTypeRuleAndDayOfWeekAndBusinessIdAndBarberIsNull(TypeRule typeRule, DayOfWeek dayOfWeek, Long businessId);

    Optional<OpeningHours> findByTypeRuleAndSpecificDateAndBusinessIdAndBarberIsNull(TypeRule typeRule, LocalDate specificDate, Long businessId);

    // Barber Hours
    List<OpeningHours> findAllByBusinessIdAndBarberId(Long businessId, Long barberId);

    List<OpeningHours> findAllByTypeRuleAndBusinessIdAndBarberId(TypeRule typeRule, Long businessId, Long barberId);

    Optional<OpeningHours> findByTypeRuleAndDayOfWeekAndBusinessIdAndBarberId(TypeRule typeRule, DayOfWeek dayOfWeek, Long businessId, Long barberId);

    Optional<OpeningHours> findByTypeRuleAndSpecificDateAndBusinessIdAndBarberId(TypeRule typeRule, LocalDate specificDate, Long businessId, Long barberId);

    Optional<OpeningHours> findByIdAndBusinessId(Long id, Long businessId);

    List<OpeningHours> findAllByTypeRule(TypeRule typeRule);

    Optional<OpeningHours> findByTypeRuleAndDayOfWeek(TypeRule typeRule, DayOfWeek dayOfWeek);

    Optional<OpeningHours> findByTypeRuleAndSpecificDate(TypeRule typeRule, LocalDate specificDate);

    Optional<OpeningHours> findByDayOfWeek(DayOfWeek dayOfWeek);
}
