package com.sixro.logistics.inventory.application.result;

import java.util.UUID;

public record InventoryCreateResult(
        UUID inventoryId,
        UUID hubId,
        UUID productId,
        Integer stock
) {

}
