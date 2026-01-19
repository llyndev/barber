package com.barbearia.barbearia.modules.business.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpensesResponse(
    Long id,
    String title,
    String description,
    String category,
    BigDecimal amount,
    LocalDate date
) {
    
}
