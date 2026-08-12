package com.sixro.logistics.delivery.application.event;

import com.sixro.logistics.delivery.domain.exception.DeliveryCreationKafkaErrorCode;

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
            DeliveryCreationKafkaErrorCode failureCode,
            String failureMessage
    ) {
    }
}
