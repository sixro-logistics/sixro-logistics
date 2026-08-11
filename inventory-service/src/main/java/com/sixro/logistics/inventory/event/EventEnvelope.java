package com.sixro.logistics.inventory.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        LocalDateTime occurredAt,
        T data
) {

    public static <T> EventEnvelope<T> of(T data) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                LocalDateTime.now(),
                data
        );
    }
}
