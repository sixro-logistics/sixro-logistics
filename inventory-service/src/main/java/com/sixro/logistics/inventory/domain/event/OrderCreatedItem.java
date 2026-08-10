package com.sixro.logistics.inventory.domain.event;

import java.util.UUID;

public record OrderCreatedItem(
        UUID productId,
        Integer quantity
) {
}
