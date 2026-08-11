package com.sixro.logistics.inventory.application.result;

import java.util.UUID;

public record InventorySearchItem(
        UUID inventoryId,
        UUID hubId,
        UUID companyId,
        UUID productId,
        Integer stock
) {
}
