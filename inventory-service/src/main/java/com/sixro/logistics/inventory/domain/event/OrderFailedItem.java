package com.sixro.logistics.inventory.domain.event;

import java.util.UUID;

public record OrderFailedItem(
        UUID productId,
        Integer quantity
) {
}
