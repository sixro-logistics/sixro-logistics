package com.sixro.logistics.delivery.infrastructure.kafka;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String ORDER_CONFIRMED = "order-confirmed";
    public static final String DELIVERY_CREATED = "delivery.created";
}
