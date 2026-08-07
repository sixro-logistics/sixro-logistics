package com.sixro.logistics.order.infrastructure.client.inventory;

import java.util.List;

public record InventoryClientResponse(
        List<InventoryClientInventory> inventories
) {
}
