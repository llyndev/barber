package com.barbearia.barbearia.modules.business.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.modules.business.model.Expenses;

public interface ExpensesRepository extends JpaRepository<Expenses, Long> {

    List<Expenses> findAllByBusinessIdOrderByDateDesc(Long businessId);

    List<Expenses> findAllByBusinessIdAndDateBetweenOrderByDateDesc(Long businessId, LocalDate startDate, LocalDate endDate);
    
    Optional<Expenses> findByIdAndBusinessId(Long id, Long businessId);
    
}
