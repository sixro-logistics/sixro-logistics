package com.sixro.logistics.inventory.infrastructure.kafka;

public class EventDeserializationException extends RuntimeException {

    public EventDeserializationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}