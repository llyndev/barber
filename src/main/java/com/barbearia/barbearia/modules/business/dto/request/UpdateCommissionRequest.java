package com.barbearia.barbearia.modules.business.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateCommissionRequest(
    @NotNull(message = "A porcentagem é obrigatória")
    @DecimalMin(value = "0.00", message = "A porcentagem mínima é 0.00")
    @DecimalMax(value = "100.00", message = "A porcentagem máxima é 100.00")
    @Digits(integer = 3, fraction = 2, message = "Formato inválido. Use até 2 casas decimais")
    BigDecimal percentage
) {}
