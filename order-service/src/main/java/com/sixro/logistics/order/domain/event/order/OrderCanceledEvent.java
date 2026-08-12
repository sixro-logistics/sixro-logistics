package com.sixro.logistics.order.domain.event.order;

import java.util.List;
import java.util.UUID;

public record OrderCanceledEvent(
        UUID orderId,
        UUID hubId,
        List<OrderCanceledItem> items
) {
}
