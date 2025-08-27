package com.barbearia.barbearia.dto.request;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SchedulingRequest {

    private Long clientId;
    private Long barberServiceId;
    private Long barberId;
    private LocalDateTime dateTime;

}
