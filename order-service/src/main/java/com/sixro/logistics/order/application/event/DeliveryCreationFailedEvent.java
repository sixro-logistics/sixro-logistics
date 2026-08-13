package com.sixro.logistics.order.application.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryCreationFailedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        DeliveryCreationFailedData data
) {
    public record DeliveryCreationFailedData(
            UUID orderId
    ) {
    }
}