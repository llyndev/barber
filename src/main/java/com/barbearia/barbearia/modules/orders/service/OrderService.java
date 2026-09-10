package com.barbearia.barbearia.modules.orders.service;

import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.business.service.BusinessService;
import com.barbearia.barbearia.modules.catalog.model.BarberService;
import com.barbearia.barbearia.modules.catalog.repository.BarberServiceRepository;
import com.barbearia.barbearia.modules.inventory.model.Product;
import com.barbearia.barbearia.modules.inventory.repository.ProductRepository;
import com.barbearia.barbearia.modules.inventory.repository.StockMovementRepository;
import com.barbearia.barbearia.modules.orders.dto.request.AddOrderItemRequest;
import com.barbearia.barbearia.modules.orders.dto.request.CheckoutRequest;
import com.barbearia.barbearia.modules.orders.dto.request.CreateOrderRequest;
import com.barbearia.barbearia.modules.orders.dto.response.OrderResponse;
import com.barbearia.barbearia.modules.orders.mapper.OrderMapper;
import com.barbearia.barbearia.modules.orders.model.*;
import com.barbearia.barbearia.modules.orders.repository.OrderRepository;
import com.barbearia.barbearia.modules.scheduling.model.AppointmentStatus;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.modules.scheduling.model.SchedulingAdditionalValue;
import com.barbearia.barbearia.modules.scheduling.repository.SchedulingRepository;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.tenant.BusinessContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private final OrderMapper orderMapper;

    private Long getBusinessId() {
        String id = BusinessContext.getBusinessId();
        if (id == null) throw new IllegalStateException("Business Context not found");
        return Long.parseLong(id);
    }

    private Order loadOpenOrder(Long orderId, Long businessId) {
        Order order = orderRepository.findByIdAndBusinessId(orderId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Order not found."));

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new InvalidRequestException("Order is not OPEN");
        }

        return order;
    }

    private void attachScheduling(Order order, Long schedulingId, Long businessId) {

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Scheduling not found."));

        if (scheduling.getStates() != AppointmentStatus.SCHEDULED) {
            throw new InvalidRequestException("Only scheduled appointments can be converted into a service ticket.");
        }

        if (orderRepository.existsBySchedulingIdAndBusinessId(schedulingId, businessId)) {
            throw new InvalidRequestException("Order already exists for this scheduling");
        }

        order.setSchedulingId(scheduling.getId());

        if (scheduling.getUser() != null) {
            order.setClientId(scheduling.getUser().getId());
            order.setClientName(scheduling.getUser().getName());
        } else {
            order.setClientName(scheduling.getClientName()); // Cliente avulso
        }

        if (scheduling.getBarber() != null) {
            order.setProfessionalId(scheduling.getBarber().getId());
        }

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
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Long businessId = getBusinessId();

        Order order = new Order();
        order.setBusinessId(businessId);
        order.setStatus(OrderStatus.OPEN);
        order.setTotalAmount(BigDecimal.ZERO);

        if (request.schedulingId() != null) {
            attachScheduling(order, request.schedulingId(), businessId);
        } else {
            // Venda em balcão sem agendamento, os dados vem do request.
            order.setClientId(request.clientId());
            order.setClientName(request.clientName());
            order.setProfessionalId(request.professionalId());
        }

        order.recalculateTotal();

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse addItem(Long orderId, AddOrderItemRequest request) {
        Long businessId = getBusinessId();
        Order order = loadOpenOrder(orderId, businessId);

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new InvalidRequestException("Invalid quantity");
        }

        String name;
        BigDecimal price;

        if (request.type() == OrderItemType.PRODUCT) {
            Product product = productRepository.findByIdAndBusinessId(request.itemId(), businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            int alreadyInOrder = order.getItems().stream()
                    .filter(i -> i.getType() == OrderItemType.PRODUCT)
                    .filter(i -> i.getItemId().equals(request.itemId()))
                    .mapToInt(OrderItem::getQuantity)
                    .sum();
            
            if (product.getQuantity() < alreadyInOrder + request.quantity()) {
                throw new IllegalStateException("Insufficient stock");
            }

            name = product.getName();
            price = product.getPrice();

        } else {
            BarberService service = barberServiceRepository.findByIdAndBusinessId(request.itemId(), businessId)
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
        order.recalculateTotal();

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse removeItem(Long orderId, Long orderItemId) {
        Long businessId = getBusinessId();
        Order order = loadOpenOrder(orderId, businessId);

        OrderItem itemToRemove = order.getItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in order"));
                
        order.removeItem(itemToRemove);
        order.recalculateTotal();

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse checkout(Long orderId, CheckoutRequest request) {
        Long businessId = getBusinessId();
        Order order = loadOpenOrder(orderId, businessId);

        if (order.getItems().isEmpty()) {
            throw new InvalidRequestException("A order without items cannot be terminated.");
        }

        List<SchedulingAdditionalValue> additionalValues = applyAdditionalValues(order, request.additionalValues(), businessId);

        order.recalculateTotal();

        applyStockExit(order, businessId);

        if (order.getSchedulingId() != null) {
            Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(order.getSchedulingId(), businessId).orElseThrow(
                    () -> new ResourceNotFoundException("Scheduling not found."));

            if (scheduling.getStates() != AppointmentStatus.SCHEDULED) {
                throw new InvalidRequestException("Scheduling already completed or cancelled.");
            }

            scheduling.setStates(AppointmentStatus.COMPLETED);
            scheduling.setPaymentMethod(request.paymentMethod());

            if (!additionalValues.isEmpty()) {
                if (scheduling.getAdditionalValue() == null) {
                    scheduling.setAdditionalValue(new ArrayList<>());
                }
                additionalValues.forEach(av -> av.setScheduling(scheduling));
                scheduling.getAdditionalValue().addAll(additionalValues);
            }

        }

        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PAID);

        return orderMapper.toResponse(orderRepository.save(order));
    }
    
    public OrderResponse getOrder(Long orderId) {
         Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
         return orderMapper.toResponse(order);
    }

    public List<OrderResponse> getOrderByBusiness(String slug) {

        var business = businessService.getBySlug(slug);

        return orderRepository.findByBusinessIdOrderByCreatedAtDesc(business.id())
            .stream()
            .map(this::toResponse)
            .toList();
    }
}
