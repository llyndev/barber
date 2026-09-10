package com.barbearia.barbearia.modules.orders.controller;

import com.barbearia.barbearia.modules.orders.dto.request.AddOrderItemRequest;
import com.barbearia.barbearia.modules.orders.dto.request.CreateOrderRequest;
import com.barbearia.barbearia.modules.orders.dto.response.OrderResponse;
import com.barbearia.barbearia.modules.orders.service.OrderService;
import com.barbearia.barbearia.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.barbearia.barbearia.modules.orders.dto.request.CheckoutRequest;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderResponse> addItem(@PathVariable Long id, @RequestBody @Valid AddOrderItemRequest request) {
        return ResponseEntity.ok(orderService.addItem(id, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderResponse> removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return ResponseEntity.ok(orderService.removeItem(id, itemId));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<OrderResponse> checkout(@PathVariable Long id, @RequestBody @Valid CheckoutRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(orderService.checkout(id, request, userDetails.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @GetMapping("/business/{slug}")
    public ResponseEntity<List<OrderResponse>> getOrderByBusiness(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderByBusiness(id));
    }
}
