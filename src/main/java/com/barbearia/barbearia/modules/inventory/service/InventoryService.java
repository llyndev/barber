package com.barbearia.barbearia.modules.inventory.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.barbearia.barbearia.exception.InsufficientStockException;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.modules.business.model.UserBusiness;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.modules.inventory.dto.request.StockMovementCommand;
import com.barbearia.barbearia.modules.inventory.dto.response.PublicProdutResponse;
import com.barbearia.barbearia.modules.inventory.event.StockDecreasedEvent;
import com.barbearia.barbearia.tenant.BusinessContext;
import com.barbearia.barbearia.tenant.BusinessGuard;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
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

import com.barbearia.barbearia.modules.inventory.dto.response.StockMovementResponse;
import com.barbearia.barbearia.modules.inventory.mapper.StockMovementMapper;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final BusinessRepository businessRepository;
    private final ProductMapper productMapper;
    private final UserBusinessRepository userBusinessRepository;
    private final StockMovementMapper stockMovementMapper;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessGuard businessGuard;


    /**
     * Listar produtos para membros da staff
     */
    public List<ProductResponse> listStaffProducts() {
        Long businessId = BusinessContext.requireBusinessId();

        businessGuard.requireOwnerOrManager();

        return productRepository.findAllByBusinessIdAndActiveTrue(businessId)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    /**
     * Listar produtos para público
     */
    public List<PublicProdutResponse> listPublicProducts() {
        Long businessId = BusinessContext.requireBusinessId();

        return productRepository.findAllByBusinessIdAndActiveTrue(businessId)
                .stream()
                .map(productMapper::toPublicResponse)
                .toList();
    }

    /**
     * Histórico de movimentações, paginado.
     */
    public Page<StockMovementResponse> listMovements(Pageable pageable) {
        Long businessId = BusinessContext.requireBusinessId();
        businessGuard.requireOwnerOrManager();

        return stockMovementRepository
                .findByProduct_Business_IdOrderByOccurredAtDesc(businessId, pageable)
                .map(stockMovementMapper::toResponse);
    }

    /**
     * Criar um novo produto para a barbearia.
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productData) {
        businessGuard.requireOwnerOrManager();
        Long businessId = BusinessContext.requireBusinessId();

        Business business = businessRepository.getReferenceById(businessId);

        Product product = productMapper.toEntity(productData);
        product.setBusiness(business);

        return productMapper.toResponse(productRepository.save(product));
    }

    /**
     * Registros de movimentação do inventario.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registerMovements(Long businessId, StockMovementType type, List<StockMovementCommand> commands, Long userId) {

        applyMovements(businessId, type, commands, userId);
    }

    /**
     * Desativar um produto a partir de seu id.
     */
    @Transactional
    public void deactivateProduct(Long productId) {
        Long businessId = BusinessContext.requireBusinessId();
        businessGuard.requireOwnerOrManager();

        Product product = productRepository.findByIdAndBusinessId(productId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));

        product.setActive(false);
    }


    /**
     *  Atualizar produto.
     * @param productId Id do produto que ira ser atualizado.
     * @param request DTO request passando parametros que podem ser atualizado.
     * @param userId Id do usuário que esta realizando essa operação.
     */
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, Long userId) {
        Long businessId = BusinessContext.requireBusinessId();
        businessGuard.requireOwnerOrManager();

        Product product = productRepository.findByIdAndBusinessId(productId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Produto não encontrado."));

        if (request.quantity() != null && !request.quantity().equals(product.getQuantity())) {

            applyMovements(
                    businessId,
                    StockMovementType.ADJUSTMENT,
                    List.of(new StockMovementCommand(product, request.quantity(), "Ajuste manual via edição de produto")),
                    userId
            );
        }

        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.sku() != null) product.setSku(request.sku());
        if (request.minQuantity() != null) product.setMinQuantity(request.minQuantity());
        if (request.price() != null) product.setPrice(request.price());
        if (request.costPrice() != null) product.setCostPrice(request.costPrice());

        return productMapper.toResponse(product);
    }

    /**
     * Movimentação manual disparada pelo painel da barbearia.
     */
    @Transactional
    public void registerManualMovement(Long productId, StockMovementType type, Integer quantity, String reason, Long currentUserId) {
        Long businessId = BusinessContext.requireBusinessId();
        businessGuard.requireOwnerOrManager();

        Product product = productRepository.findByIdAndBusinessId(productId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Produto não encontrado."));

        applyMovements(businessId, type, List.of(new StockMovementCommand(product, quantity, reason)), currentUserId);
    }

    /**
     * Faz as verificações nescessarias para aplicar as movimentações no estoque.
     */
    private void applyMovements(Long businessId, StockMovementType type, List<StockMovementCommand> commands, Long userId) {
        if (commands == null || commands.isEmpty()) return;

        UserBusiness membership = userBusinessRepository.findByUserIdAndBusinessId(userId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Usuário não possuí vínculo com esta barbearia."));

        List<StockMovementCommand> ordered = commands.stream()
                .sorted(Comparator.comparing(command -> command.product().getId()))
                .toList();

        List<StockMovement> movements = new ArrayList<>(ordered.size());

        for (StockMovementCommand command : ordered) {
            Integer amount = command.quantity();

            if (amount == null || amount < 0) {
                throw new InvalidRequestException("Quantidade inválida para o produto " + command.product().getName());
            }

            if (amount == 0 && type != StockMovementType.ADJUSTMENT) {
                throw new InvalidRequestException("Quantidade deve ser maior que zero para " + command.product().getName());
            }

            Product product = productRepository.findByIdAndBusinessIdForUpdate(command.product().getId(), businessId).orElseThrow(
                    () -> new ResourceNotFoundException("Produto não encontrado: " + command.product().getId()));

            int previous = product.getQuantity();

            int next = switch (type) {
                case EXIT -> {
                    if (previous < amount) {
                        throw new InsufficientStockException("Estoque insuficiente para o produto: " + product.getName() + " (disponível: " + previous + ", solicitado: " + amount + ")");
                    }
                    yield previous - amount;
                }
                case ENTRY -> previous + amount;

                case ADJUSTMENT -> amount;
            };

            product.setQuantity(next);

            movements.add(StockMovement.builder()
                    .product(product)
                    .type(type)
                    .quantity(Math.abs(next - previous))
                    .previousQuantity(previous)
                    .newQuantity(next)
                    .reason(command.reason())
                    .performedBy(membership)
                    .build());
        }

        stockMovementRepository.saveAll(movements);

        if (type == StockMovementType.EXIT || type == StockMovementType.ADJUSTMENT) {
            Set<Long> touchedIds = ordered.stream()
                    .map(command -> command.product().getId())
                    .collect(Collectors.toSet());

            eventPublisher.publishEvent(new StockDecreasedEvent(businessId, touchedIds));
        }
    }

}
