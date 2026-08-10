package com.sixro.logistics.inventory.domain.event;

import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID hubId,
        List<OrderCreatedItem> items
) {
}
