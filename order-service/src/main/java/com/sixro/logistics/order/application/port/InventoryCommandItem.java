package com.sixro.logistics.order.application.port;

import java.util.UUID;

public record InventoryCommandItem(
        UUID productId,
        Integer quantity
) {
}
