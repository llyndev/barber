package com.barbearia.barbearia.modules.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Nome é obrigatório.")
        String name,
        String description,
        String sku,

        @PositiveOrZero(message = "Quantidade não pode ser negativa.")
        Integer quantity,

        @PositiveOrZero(message = "Quantidade não pode ser negativa.")
        Integer minQuantity,

        @NotNull(message = "Preço é obrigatório.")
        @DecimalMin(value = "0.0", inclusive = false, message = "Preço deve ser maior que zero.")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "Preço de custo não pode ser negativo.")
        BigDecimal costPrice
) {
    
}
