package com.sixro.logistics.order.domain.event.order;

import java.util.List;
import java.util.UUID;

public record OrderFailedEvent(
        UUID orderId,
        UUID hubId,
        List<OrderFailedItem> items
) {
}
