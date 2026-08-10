package com.sixro.logistics.delivery.infrastructure.kafka.event;

import java.util.UUID;

public record OrderConfirmedItem(
        UUID companyId,
        UUID productId,
        Integer quantity
) {
}
