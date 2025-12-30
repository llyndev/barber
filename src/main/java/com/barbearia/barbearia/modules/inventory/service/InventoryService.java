package com.barbearia.barbearia.modules.inventory.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.inventory.dto.request.ProductRequest;
import com.barbearia.barbearia.modules.inventory.dto.response.ProductResponse;
import com.barbearia.barbearia.modules.inventory.mapper.ProductMapper;
import com.barbearia.barbearia.modules.inventory.model.Product;
import com.barbearia.barbearia.modules.inventory.model.StockMovement;
import com.barbearia.barbearia.modules.inventory.model.StockMovementType;
import com.barbearia.barbearia.modules.inventory.repository.ProductRepository;
import com.barbearia.barbearia.modules.inventory.repository.StockMovementRepository;
import com.barbearia.barbearia.modules.business.service.BusinessService;

import com.barbearia.barbearia.modules.inventory.dto.response.StockMovementResponse;
import com.barbearia.barbearia.modules.inventory.mapper.StockMovementMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {
    
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final BusinessRepository businessRepository;
    private final ProductMapper productMapper;
    private final BusinessService businessService;
    private final StockMovementMapper stockMovementMapper;

    public List<ProductResponse> listProducts(String slug) {
        Business business = businessRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado"));

        return productRepository.findAllByBusinessIdAndActiveTrue(business.getId())
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse createProduct(String slug, ProductRequest productData, AppUser user) {
        Business business = businessService.validateOwnerOrManagerBySlug(slug, user.getId());

        Product product = productMapper.toEntity(productData);
        product.setBusiness(business);

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public BigDecimal registerMovement(String slug, Long productId, StockMovementType type, Integer quantity, String reason, AppUser user) {
        Business business = businessRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado"));

        Product product = productRepository.findByIdAndBusinessId(productId, business.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado para este business"));

        if (quantity <= 0) {
            throw new InvalidRequestException("Quantity must be greater than zero");
        }

        if (type == StockMovementType.ENTRY) {
            product.setQuantity(product.getQuantity() + quantity);
        } else if (type == StockMovementType.EXIT) {
            if (product.getQuantity() < quantity) {
                throw new InvalidRequestException("Estoque insuficiente");
            }
            product.setQuantity(product.getQuantity() - quantity);
        } else if (type == StockMovementType.ADJUSTMENT) {
            product.setQuantity(quantity);
        }

        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .type(type)
                .quantity(quantity)
                .reason(reason)
                .user(user)
                .build();
        
        stockMovementRepository.save(movement);

        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public List<StockMovementResponse> listMovements(String slug) {
        Business business = businessRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        return stockMovementRepository.findByProduct_Business_IdOrderByDateDesc(business.getId())
                .stream()
                .map(stockMovementMapper::toResponse)
                .toList();
    }

    public void deactivateProduct(String slug, Long productId, AppUser user) {
        
        Business business = businessService.validateOwnerOrManagerBySlug(slug, user.getId());

        Product product = productRepository.findByIdAndBusinessId(productId, business.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found for this business"));

        product.setActive(false);
        productRepository.save(product);
    }

    public ProductResponse updateProduct(String slug, Long productId, ProductRequest request, AppUser user) {
        Business business = businessService.validateOwnerOrManagerBySlug(slug, user.getId());

        Product product = productRepository.findByIdAndBusinessId(productId, business.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (request.quantity() != null && !request.quantity().equals(product.getQuantity())) {
            StockMovement movement = StockMovement.builder()
                .product(product)
                .type(StockMovementType.ADJUSTMENT)
                .quantity(request.quantity())
                .reason("Manual update via Edit Product")
                .user(user)
                .build();
            stockMovementRepository.save(movement);
            product.setQuantity(request.quantity());
        }

        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.sku() != null) product.setSku(request.sku());
        if (request.minQuantity() != null) product.setMinQuantity(request.minQuantity());
        if (request.price() != null) product.setPrice(request.price());
        if (request.costPrice() != null) product.setCostPrice(request.costPrice());

        return productMapper.toResponse(productRepository.save(product));
    }

}
