package com.barbearia.barbearia.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BarberServiceResponse {

    private Long id;
    private String nameService;
    private String description;
    private int durationInMinutes;
    private BigDecimal price;
}
