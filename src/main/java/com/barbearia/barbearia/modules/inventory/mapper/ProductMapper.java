package com.barbearia.barbearia.modules.inventory.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.modules.inventory.dto.request.ProductRequest;
import com.barbearia.barbearia.modules.inventory.dto.response.ProductResponse;
import com.barbearia.barbearia.modules.inventory.model.Product;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        if (request == null) {
            return null;
        }

        return Product.builder()
                .name(request.name())
                .description(request.description())
                .sku(request.sku())
                .quantity(request.quantity() != null ? request.quantity() : 0)
                .minQuantity(request.minQuantity())
                .price(request.price())
                .costPrice(request.costPrice())
                .active(true)
                .build();
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getSku(),
            product.getQuantity(),
            product.getMinQuantity(),
            product.getPrice(),
            product.getCostPrice(),
            product.isActive()
        );
    }
}
