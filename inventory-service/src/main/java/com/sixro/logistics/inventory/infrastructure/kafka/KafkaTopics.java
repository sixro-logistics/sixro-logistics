package com.sixro.logistics.inventory.infrastructure.kafka;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String ORDER_CREATED =
            "order-created";

    public static final String INVENTORY_DEDUCTED =
            "inventory-deducted";

    public static final String INVENTORY_DEDUCTION_FAILED =
            "inventory-deduction-failed";
}