package com.barbearia.barbearia.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SchedulingResponse {

    private Long id;
    private LocalDateTime dateTime;
    private Long clientId;
    private String clientName;
    private Long barberId;
    private String barberName;
    private Long serviceId;
    private String serviceName;

}
