package com.sixro.logistics.inventory.infrastructure.kafka;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String ORDER_CANCELED = "order.canceled";

    public static final String ORDER_FAILED = "order.failed";

}