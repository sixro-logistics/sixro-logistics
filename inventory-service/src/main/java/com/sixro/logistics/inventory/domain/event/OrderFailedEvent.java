package com.sixro.logistics.inventory.domain.event;

import java.util.List;
import java.util.UUID;

public record OrderFailedEvent(
        UUID orderId,
        UUID hubId,
        List<OrderFailedItem> items
) {
}
