package com.sixro.logistics.inventory.domain.event;

import java.util.UUID;

public record InventoryDeductedEvent(
        UUID orderId
) {
}