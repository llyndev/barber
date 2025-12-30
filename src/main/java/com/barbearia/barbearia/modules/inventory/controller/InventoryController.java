package com.barbearia.barbearia.modules.inventory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.barbearia.barbearia.modules.inventory.dto.request.MovementRequest;
import com.barbearia.barbearia.modules.inventory.dto.request.ProductRequest;
import com.barbearia.barbearia.modules.inventory.dto.response.ProductResponse;
import com.barbearia.barbearia.modules.inventory.dto.response.StockMovementResponse;
import com.barbearia.barbearia.modules.inventory.service.InventoryService;
import com.barbearia.barbearia.security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryService inventoryService;

    @GetMapping("/{slug}")
    public ResponseEntity<List<ProductResponse>> listProducts(@PathVariable String slug) {
        return ResponseEntity.ok(inventoryService.listProducts(slug));
    }

    @PostMapping("/{slug}")
    public ResponseEntity<ProductResponse> createProduct(@PathVariable String slug, @RequestBody ProductRequest product, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(inventoryService.createProduct(slug, product, userDetails.user()));
    }

    @PutMapping("/{slug}/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String slug, 
            @PathVariable Long productId, 
            @RequestBody ProductRequest product,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(inventoryService.updateProduct(slug, productId, product, userDetails.user()));
    }

    @PostMapping("/{slug}/{productId}/movement")
    public ResponseEntity<Void> registerMovement(
            @PathVariable String slug, 
            @PathVariable Long productId, 
            @RequestBody MovementRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        inventoryService.registerMovement(slug, productId, request.type(), request.quantity(), request.reason(), userDetails.user());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{slug}/history")
    public ResponseEntity<List<StockMovementResponse>> listHistory(@PathVariable String slug) {
        return ResponseEntity.ok(inventoryService.listMovements(slug));
    }

    @DeleteMapping("/{slug}/{productId}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable String slug, @PathVariable Long productId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        inventoryService.deactivateProduct(slug, productId, userDetails.user());
        return ResponseEntity.noContent().build();
    }

}
