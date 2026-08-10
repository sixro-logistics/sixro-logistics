package com.sixro.logistics.order.infrastructure.client.inventory;

import java.util.List;
import java.util.UUID;

public record InventoryClientResponse(
        UUID hubId,
        List<InventoryClientItem> inventories
) {
}
