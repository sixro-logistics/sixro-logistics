package com.sixro.logistics.order.domain.event.order;

import java.util.UUID;

public record OrderCreatedItem(
        UUID companyId,
        UUID productId,
        Integer quantity
) {
}
