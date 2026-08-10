package com.sixro.logistics.order.infrastructure.client.inventory;

import java.util.UUID;

public record InventoryClientItem(
        UUID productId,
        Integer stock
) {}