package com.sixro.logistics.order.infrastructure.client.inventory;

import java.util.UUID;

public record InventoryClientInventory(
        UUID hubId,
        UUID productId,
        Integer stock
) {}