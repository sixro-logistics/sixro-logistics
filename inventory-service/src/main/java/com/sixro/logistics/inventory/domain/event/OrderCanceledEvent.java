package com.sixro.logistics.inventory.domain.event;

import java.util.List;
import java.util.UUID;

public record OrderCanceledEvent(
        UUID orderId,
        UUID hubId,
        List<OrderCanceledItem> items
) {
}
