package com.sixro.logistics.order.infrastructure.client.inventory;

import java.util.UUID;

public record InventoryRequestItem(
        UUID productId,
        Integer quantity
) {
}
