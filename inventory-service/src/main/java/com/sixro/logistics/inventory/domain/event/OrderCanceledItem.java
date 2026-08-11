package com.sixro.logistics.inventory.domain.event;

import java.util.UUID;

public record OrderCanceledItem(
        UUID productId,
        Integer quantity
) {
}
