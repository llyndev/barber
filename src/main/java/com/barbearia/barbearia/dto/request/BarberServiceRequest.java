package com.barbearia.barbearia.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarberServiceRequest {

    private String nameService;
    private String description;
    private int durationInMinutes;
    private BigDecimal price;

}
