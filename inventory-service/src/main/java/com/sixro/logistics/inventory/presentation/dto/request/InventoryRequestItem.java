package com.sixro.logistics.inventory.presentation.dto.request;

import java.util.UUID;

public record InventoryRequestItem(
        UUID productId,
        Integer quantity
) {
}
