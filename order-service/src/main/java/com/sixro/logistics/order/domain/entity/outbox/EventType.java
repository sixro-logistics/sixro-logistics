package com.sixro.logistics.order.domain.entity.outbox;

public enum EventType {
    ORDER_CREATED,
    ORDER_FAILED,
    ORDER_CANCELED
}