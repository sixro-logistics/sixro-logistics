package com.sixro.logistics.inventory.infrastructure.kafka;

import com.sixro.logistics.inventory.domain.event.InventoryDeductedEvent;
import com.sixro.logistics.inventory.domain.event.InventoryDeductionFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class KafkaProducer {

    // Spring Kafka에서 Kafka에 메시지를 send, publish할 수 있도록 제공
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendInventoryDeductedEvent(
            InventoryDeductedEvent event
    ) throws ExecutionException, InterruptedException {

        kafkaTemplate.send(
                KafkaTopics.INVENTORY_DEDUCTED,
                event.orderId().toString(),
                event
        ).get();
    }

    public void sendInventoryDeductionFailedEvent(
            InventoryDeductionFailedEvent event
    ) throws ExecutionException, InterruptedException {

        kafkaTemplate.send(
                KafkaTopics.INVENTORY_DEDUCTION_FAILED,
                event.orderId().toString(),
                event
        ).get();
    }
}