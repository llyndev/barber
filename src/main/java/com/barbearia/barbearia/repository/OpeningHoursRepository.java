package com.barbearia.barbearia.repository;

import com.barbearia.barbearia.model.OpeningHours;
import com.barbearia.barbearia.model.OpeningHours.TypeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OpeningHoursRepository extends JpaRepository<OpeningHours, Long> {

    List<OpeningHours> findAllByTypeRule(TypeRule typeRule);

    Optional<OpeningHours> findByTypeRuleAndDayOfWeek(TypeRule typeRule, DayOfWeek dayOfWeek);

    Optional<OpeningHours> findByTypeRuleAndSpecificDate(TypeRule typeRule, LocalDate specificDate);

    Optional<OpeningHours> findByDayOfWeek(DayOfWeek dayOfWeek);
}
