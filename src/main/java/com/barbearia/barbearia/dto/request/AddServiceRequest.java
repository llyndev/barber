package com.barbearia.barbearia.dto.request;

import java.util.List;

public record AddServiceRequest(
        List<Long> barberServiceIds
) {
}
