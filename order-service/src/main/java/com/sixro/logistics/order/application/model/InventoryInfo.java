package com.sixro.logistics.order.application.model;

import java.util.UUID;

public record InventoryInfo(
        UUID hubId,
        UUID productId,
        Integer stock
) {
}
