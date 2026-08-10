package com.sixro.logistics.inventory.domain.event;

import java.util.UUID;

public record InventoryDeductionFailedEvent(
        UUID orderId,
        String errorCode,
        String failureReason
) {
}