package com.barbearia.barbearia.modules.orders.service;

import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.business.service.BusinessService;
import com.barbearia.barbearia.modules.catalog.model.BarberService;
import com.barbearia.barbearia.modules.catalog.repository.BarberServiceRepository;
import com.barbearia.barbearia.modules.inventory.model.Product;
import com.barbearia.barbearia.modules.inventory.model.StockMovement;
import com.barbearia.barbearia.modules.inventory.model.StockMovementType;
import com.barbearia.barbearia.modules.inventory.repository.ProductRepository;
import com.barbearia.barbearia.modules.inventory.repository.StockMovementRepository;
import com.barbearia.barbearia.modules.orders.dto.request.AddOrderItemRequest;
import com.barbearia.barbearia.modules.orders.dto.request.CheckoutRequest;
import com.barbearia.barbearia.modules.orders.dto.request.CreateOrderRequest;
import com.barbearia.barbearia.modules.orders.dto.response.OrderItemResponse;
import com.barbearia.barbearia.modules.orders.dto.response.OrderResponse;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingAdditionalValueResponse;
import com.barbearia.barbearia.modules.orders.model.*;
import com.barbearia.barbearia.modules.orders.repository.OrderRepository;
import com.barbearia.barbearia.modules.scheduling.model.AppointmentStatus;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.modules.scheduling.model.SchedulingAdditionalValue;
import com.barbearia.barbearia.modules.scheduling.repository.SchedulingRepository;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.tenant.BusinessContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final SchedulingRepository schedulingRepository;
    private final ProductRepository productRepository;
    private final BarberServiceRepository barberServiceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final BusinessService businessService;
    private final UserRepository userRepository;

    private Long getBusinessId() {
        String id = BusinessContext.getBusinessId();
        if (id == null) throw new IllegalStateException("Business Context not found");
        return Long.parseLong(id);
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Long businessId = getBusinessId();
        Order order = new Order();
        order.setBusinessId(businessId);
        order.setStatus(OrderStatus.OPEN);
        order.setTotalAmount(BigDecimal.ZERO);

        if (request.schedulingId() != null) {
            Scheduling scheduling = schedulingRepository.findById(request.schedulingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Scheduling not found"));
            
            if (orderRepository.findBySchedulingId(request.schedulingId()).isPresent()) {
                 throw new IllegalStateException("Order already exists for this scheduling");
            }

            order.setSchedulingId(scheduling.getId());
            if (scheduling.getUser() != null) {
                order.setClientId(scheduling.getUser().getId());
                order.setClientName(scheduling.getUser().getName());
            } else {
                order.setClientName(scheduling.getClientName());
            }
            order.setProfessionalId(scheduling.getBarber().getId());

            if (scheduling.getBarberService() != null) {
                for (BarberService service : scheduling.getBarberService()) {
                    OrderItem item = OrderItem.builder()
                            .type(OrderItemType.SERVICE)
                            .itemId(service.getId())
                            .name(service.getNameService())
                            .quantity(1)
                            .unitPrice(service.getPrice())
                            .build();
                    item.calculateTotal();
                    order.addItem(item);
                }
            }

            if (scheduling.getAdditionalValue() != null && scheduling.getAdditionalValue().compareTo(BigDecimal.ZERO) > 0) {
                OrderItem additionalItem = OrderItem.builder()
                        .type(OrderItemType.ADDITIONAL)
                        .itemId(0L)
                        .name("Valor Adicional")
                        .quantity(1)
                        .unitPrice(scheduling.getAdditionalValue())
                        .build();
                additionalItem.calculateTotal();
                order.addItem(additionalItem);
            }
        } else {
            order.setClientId(request.clientId());
            order.setClientName(request.clientName());
            order.setProfessionalId(request.professionalId());
        }

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional void removeItem(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new IllegalStateException("Order is not OPEN");
        }
        
        
    }

    @Transactional
    public OrderResponse addItem(Long orderId, AddOrderItemRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new IllegalStateException("Order is not OPEN");
        }

        String name;
        BigDecimal price;

        if (request.type() == OrderItemType.PRODUCT) {
            Product product = productRepository.findById(request.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            name = product.getName();
            price = product.getPrice();
            
            if (product.getQuantity() < request.quantity()) {
                throw new IllegalStateException("Insufficient stock");
            }

        } else {
            BarberService service = barberServiceRepository.findById(request.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
            name = service.getNameService();
            price = service.getPrice();
        }

        OrderItem item = OrderItem.builder()
                .type(request.type())
                .itemId(request.itemId())
                .name(name)
                .quantity(request.quantity())
                .unitPrice(price)
                .build();
        item.calculateTotal();
        
        order.addItem(item);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse removeItem(Long orderId, Long orderItemId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new IllegalStateException("Order is not OPEN");
        }

        OrderItem itemToRemove = order.getItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in order"));
                
        order.removeItem(itemToRemove);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse checkout(Long orderId, CheckoutRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new IllegalStateException("Order is not OPEN");
        }

        for (OrderItem item : order.getItems()) {
            if (item.getType() == OrderItemType.PRODUCT) {
                Product product = productRepository.findById(item.getItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.getName()));
                
                int newQuantity = product.getQuantity() - item.getQuantity();
                if (newQuantity < 0) {
                     throw new IllegalStateException("Insufficient stock for: " + item.getName());
                }
                product.setQuantity(newQuantity);
                productRepository.save(product);

                StockMovement movement = StockMovement.builder()
                        .product(product)
                        .type(StockMovementType.EXIT)
                        .quantity(item.getQuantity())
                        .reason("Order Checkout #" + order.getId())
                        .date(java.time.LocalDateTime.now())
                        .build();
                stockMovementRepository.save(movement);
            }
        }

        if (order.getSchedulingId() != null) {
            Scheduling scheduling = schedulingRepository.findById(order.getSchedulingId())
                    .orElse(null);
            if (scheduling != null) {
                scheduling.setStates(AppointmentStatus.COMPLETED);

                if (request.additionalValues() != null && !request.additionalValues().isEmpty()) {
                    if (scheduling.getAdditionalValues() == null) {
                        scheduling.setAdditionalValues(new java.util.ArrayList<>());
                    }
                    for (CheckoutRequest.AdditionalValueRequest val : request.additionalValues()) {
                        AppUser barber = userRepository.findById(val.barberId())
                            .orElseThrow(() -> new ResourceNotFoundException("Barber not found ID: " + val.barberId()));

                        SchedulingAdditionalValue additionalValue = SchedulingAdditionalValue.builder()
                            .scheduling(scheduling)
                            .barber(barber)
                            .value(val.value())
                            .build();
                        
                        scheduling.getAdditionalValues().add(additionalValue);
                    }
                }

                scheduling.setPaymentMethod(request.paymentMethod());
                schedulingRepository.save(scheduling);
            }
        }

        order.setStatus(OrderStatus.PAID);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }
    
    public OrderResponse getOrder(Long orderId) {
         Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
         return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
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

    public List<OrderResponse> getOrderByBusiness(String slug) {

        var business = businessService.getBySlug(slug);

        return orderRepository.findByBusinessIdOrderByCreatedAtDesc(business.id())
            .stream()
            .map(this::toResponse)
            .toList();
    }
}
