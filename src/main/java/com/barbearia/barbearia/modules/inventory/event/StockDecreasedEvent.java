package com.barbearia.barbearia.modules.inventory.event;

import java.util.Set;

public record StockDecreasedEvent(Long businessId, Set<Long> productIds) {
}
