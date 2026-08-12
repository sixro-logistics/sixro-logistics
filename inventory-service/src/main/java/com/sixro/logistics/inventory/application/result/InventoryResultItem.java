package com.sixro.logistics.inventory.application.result;

import java.util.UUID;

public record InventoryResultItem(
        UUID productId,
        Integer stock
) {
}
