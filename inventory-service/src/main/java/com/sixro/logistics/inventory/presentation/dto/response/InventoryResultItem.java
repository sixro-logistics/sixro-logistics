package com.sixro.logistics.inventory.presentation.dto.response;

import java.util.UUID;

public record InventoryResultItem(
        UUID productId,
        Integer stock
) {
}
