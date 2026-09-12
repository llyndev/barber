package com.barbearia.barbearia.modules.inventory.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.modules.inventory.dto.request.StockMovementCommand;
import com.barbearia.barbearia.modules.inventory.dto.response.PublicProdutResponse;
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

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class InventoryService {
    
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final BusinessRepository businessRepository;
    private final ProductMapper productMapper;
    private final BusinessService businessService;
    private final StockMovementMapper stockMovementMapper;
    private final UserRepository userRepository;

    public List<ProductResponse> listProducts(String slug, AppUser user) {
        Business business = businessRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado"));

        businessService.validateOwnerOrManagerBySlug(slug, user.getId());

        return productRepository.findAllByBusinessIdAndActiveTrue(business.getId())
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public List<PublicProdutResponse> publicListProducts(String slug) {
        Business business = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        return productRepository.findAllByBusinessIdAndActiveTrue(business.getId())
                .stream()
                .map(productMapper::toPublicResponse)
                .toList();
    }

    public ProductResponse createProduct(String slug, ProductRequest productData, AppUser user) {
        Business business = businessService.validateOwnerOrManagerBySlug(slug, user.getId());

        Product product = productMapper.toEntity(productData);
        product.setBusiness(business);

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registerMovements(Long businessId, StockMovementType type, List<StockMovementCommand> commands, Long perfomedByUserId) {

        if (commands == null || commands.isEmpty()) return;

        AppUser performedBy = userRepository.getReferenceById(perfomedByUserId);

        List<StockMovementCommand> ordered = commands.stream()
                .sorted(Comparator.comparing(command -> command.product().getId()))
                .toList();

        List<StockMovement> movements = new ArrayList<>(ordered.size());
        LocalDateTime now = LocalDateTime.now();

        for (StockMovementCommand command : ordered) {
            Product product = command.product();
            Integer quantity = command.quantity();

            if (quantity == null || quantity <= 0) {
                throw new InvalidRequestException(
                        "Quantidade inválida para o produto " + product.getName()
                );
            }

            switch (type) {
                case EXIT -> {
                    int update = productRepository.decreaseStock(product.getId(), businessId, quantity);

                    if (update == 0) {
                        throw new InsufficientStockException(
                                "Insufficient stock for: " + product.getName()
                        );
                    }
                }
                case ENTRY -> productRepository.increaseStock(product.getId(), businessId, quantity);

                default -> throw new InvalidRequestException("Unsupported movement type: " + type);
            }

            movements.add(StockMovement.builder()
                    .product(product)
                    .type(type)
                    .quantity(quantity)
                    .reason(command.reason())
                    .date(now)
                    .user(performedBy)
                    .build());
        }

        stockMovementRepository.saveAll(movements);
    }


    public List<StockMovementResponse> listMovements(String slug, AppUser user) {
        Business business = businessRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        businessService.validateOwnerOrManagerBySlug(slug, user.getId());

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
