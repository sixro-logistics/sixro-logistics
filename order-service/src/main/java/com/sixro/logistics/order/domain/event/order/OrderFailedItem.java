package com.sixro.logistics.order.domain.event.order;

import java.util.UUID;

public record OrderFailedItem(
        UUID productId,
        Integer quantity
) {
}
