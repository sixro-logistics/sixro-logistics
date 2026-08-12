package com.sixro.logistics.order.infrastructure.kafka;

import com.sixro.logistics.order.domain.event.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreatedEvent(OrderCreatedEvent event)
            throws ExecutionException, InterruptedException {

        kafkaTemplate.send(
                KafkaTopics.ORDER_CREATED,
                event.orderId().toString(),
                event
        ).get();
    }

    // TODO: OrderCancelledEvent 구현
}