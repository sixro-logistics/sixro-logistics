package com.sixro.logistics.delivery.infrastructure.kafka;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String ORDER_CREATED = "order.created";
    public static final String DELIVERY_CREATED = "delivery.created";
}
