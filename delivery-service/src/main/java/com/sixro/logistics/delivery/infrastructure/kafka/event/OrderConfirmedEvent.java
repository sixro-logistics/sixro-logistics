package com.sixro.logistics.delivery.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        OrderConfirmedData data
) {
}
