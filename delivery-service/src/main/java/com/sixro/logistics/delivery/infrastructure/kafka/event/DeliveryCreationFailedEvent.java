package com.sixro.logistics.delivery.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryCreationFailedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        DeliveryCreationFailedData data
) {

    public record DeliveryCreationFailedData(
            UUID orderCreatedEventId,
            UUID orderId,
            DeliveryCreationFailureCode failureCode,
            String failureMessage
    ) {
    }
}
