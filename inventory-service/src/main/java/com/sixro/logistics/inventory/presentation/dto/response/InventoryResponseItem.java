package com.sixro.logistics.inventory.presentation.dto.response;

import java.util.UUID;

public record InventoryResponseItem(
        UUID productId,
        Integer stock
) {
}
