package com.barbearia.barbearia.modules.orders.mapper;

import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.modules.orders.dto.response.OrderItemResponse;
import com.barbearia.barbearia.modules.orders.dto.response.OrderResponse;
import com.barbearia.barbearia.modules.orders.model.Order;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingAdditionalValueResponse;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.modules.scheduling.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final UserRepository userRepository;
    private final SchedulingRepository schedulingRepository;

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getId(),
                        i.getType(),
                        i.getItemId(),
                        i.getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getTotalPrice()
                ))
                .collect(Collectors.toList());

        String clientName = order.getClientName();
        if (clientName == null && order.getClientId() != null) {
            clientName = userRepository.findById(order.getClientId())
                    .map(user -> user.getName())
                    .orElse(null);
        }

        String professionalName = order.getProfessionalId() != null
                ? userRepository.findById(order.getProfessionalId())
                .map(user -> user.getName())
                .orElse(null)
                : null;

        List<SchedulingAdditionalValueResponse> additionalValues = List.of();
        if (order.getSchedulingId() != null) {
            Scheduling s = schedulingRepository.findById(order.getSchedulingId()).orElse(null);
            if (s != null && s.getAdditionalValues() != null) {
                additionalValues = s.getAdditionalValues().stream()
                        .map(av -> new SchedulingAdditionalValueResponse(
                                av.getId(),
                                av.getBarber().getId(),
                                av.getBarber().getName(),
                                av.getValue()
                        ))
                        .collect(Collectors.toList());
            }
        }

        return new OrderResponse(
                order.getId(),
                order.getBusinessId(),
                order.getClientId(),
                clientName,
                order.getProfessionalId(),
                professionalName,
                order.getSchedulingId(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                additionalValues
        );
    }
}
