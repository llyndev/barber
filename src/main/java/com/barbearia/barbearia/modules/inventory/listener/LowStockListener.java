package com.barbearia.barbearia.modules.inventory.listener;

import com.barbearia.barbearia.modules.inventory.event.StockDecreasedEvent;
import com.barbearia.barbearia.modules.inventory.model.Product;
import com.barbearia.barbearia.modules.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 Listener responsável por verificar produtos com estoque baixo após uma
 venda ter sido confirmada (AFTER_COMMIT).
 Após o commit, consulta os produtos afetados em uma nova transação
 somente leitura (REQUIRES_NEW) e registra um alerta quando a quantidade
 fica abaixo do estoque mínimo.
 Falhas na verificação não afetam a venda já concluída, sendo apenas
 registradas no log.
 Futuramente, o log pode ser substituído por uma notificação real,
 como push ou e-mail para o responsável pelo estabelecimento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockListener {

    private final ProductRepository productRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onStockDecreased(StockDecreasedEvent event) {
        try {
            List<Product> lowStock = productRepository
                    .findLowStock(event.productIds(), event.businessId());

            if (lowStock.isEmpty()) return;

            lowStock.forEach(product -> log.warn(
                            "Estoque baixo — business {}, produto {} ({}): {} restante(s), mínimo {}",
                    event.businessId(), product.getName(), product.getId(),
                    product.getQuantity(), product.getMinQuantity()));

        } catch (Exception ex) {
            log.error("Falha ao verificar estoque baixo: {}", ex.getMessage(), ex);
        }
    }
}
