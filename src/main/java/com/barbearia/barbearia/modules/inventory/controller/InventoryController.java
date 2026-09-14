package com.barbearia.barbearia.modules.inventory.controller;

import java.util.List;

import com.barbearia.barbearia.modules.inventory.dto.response.PublicProdutResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryService inventoryService;

    /**
     * Lista todos os produtos ao público.
     */
    @GetMapping("/public")
    public ResponseEntity<List<PublicProdutResponse>> listPublicProducts() {
        return ResponseEntity.ok(inventoryService.listPublicProducts());
    }

    /**
     * Lista todos os produtos para os staffs com o response completo de Product.
     */
    @GetMapping()
    public ResponseEntity<List<ProductResponse>> listStaffProducts() {
        return ResponseEntity.ok(inventoryService.listStaffProducts());
    }

    /**
     * Cria um novo produto.
     */
    @PostMapping()
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest product) {
        return ResponseEntity.ok(inventoryService.createProduct(product));
    }

    /**
     * Atualizar um produto existente em uma barbearia.
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId, 
            @RequestBody ProductRequest product,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(inventoryService.updateProduct(productId, product, userDetails.id()));
    }

    /**
     * Registra uma movimentação manual no invetario.
     */
    @PostMapping("/{productId}/movement")
    public ResponseEntity<Void> registerManualMovement(
            @PathVariable Long productId, 
            @RequestBody MovementRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        inventoryService.registerManualMovement(productId, request.type(), request.quantity(), request.reason(), userDetails.id());
        return ResponseEntity.ok().build();
    }

    /**
     * Lista todo o historico de movimentação do inventario.
     */
    @GetMapping("/history")
    public ResponseEntity<Page<StockMovementResponse>> listHistory(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.listMovements(pageable));
    }

    /**
     * Desativa um produto por id.
     */
    @DeleteMapping("/{productId}/deactivate")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long productId) {
        inventoryService.deactivateProduct(productId);
        return ResponseEntity.noContent().build();
    }

}
