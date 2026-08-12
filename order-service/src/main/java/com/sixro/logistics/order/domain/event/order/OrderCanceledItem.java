package com.sixro.logistics.order.domain.event.order;

import java.util.UUID;

public record OrderCanceledItem(
        UUID productId,
        Integer quantity
) {
}
