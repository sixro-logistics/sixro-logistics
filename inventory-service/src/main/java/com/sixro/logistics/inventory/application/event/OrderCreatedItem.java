package com.sixro.logistics.inventory.application.event;

import java.util.UUID;

public record OrderCreatedItem(
        UUID productId,
        Integer quantity
) {
}
