package com.sixro.logistics.order.infrastructure.kafka;

public final class KafkaTopics {

    public static final String ORDER_CREATED = "order.created";

    public static final String ORDER_CANCELED = "order.canceled";

    public static final String ORDER_FAILED = "order.failed";

    public static final String DELIVERY_CREATED = "delivery.created";

    public static final String DELIVERY_CREATION_FAILED = "delivery.creation.failed";

    private KafkaTopics() {
    }
}