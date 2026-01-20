package com.barbearia.barbearia.modules.business.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExpensesRequest(

    @NotBlank(message = "O título é obrigatório")
    String title,

    String description,

    @NotBlank(message = "A categoria é obrigatória")
    String category,

    @NotNull(message = "O valor é obrigatório")
    @Positive
    BigDecimal amount,

    @NotNull(message = "A data é obrigatória")
    LocalDate date
) {
    
}
