package com.sixro.logistics.order.application.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryCreatedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        DeliveryCreatedData data
) {
    public record DeliveryCreatedData(
            UUID orderId,
            UUID deliveryId
    ) {
    }
}