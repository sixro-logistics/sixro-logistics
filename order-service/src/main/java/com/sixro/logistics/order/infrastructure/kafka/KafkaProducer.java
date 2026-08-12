package com.sixro.logistics.order.infrastructure.kafka;

import com.sixro.logistics.order.application.event.EventEnvelope;
import com.sixro.logistics.order.domain.event.order.OrderCanceledEvent;
import com.sixro.logistics.order.domain.event.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreatedEvent(EventEnvelope<OrderCreatedEvent> event)
            throws ExecutionException, InterruptedException {

        kafkaTemplate.send(
                KafkaTopics.ORDER_CREATED,
                event.data().orderId().toString(),
                event
        ).get();
    }

    public void sendOrderCanceledEvent(EventEnvelope<OrderCanceledEvent> event)
            throws ExecutionException, InterruptedException {

        kafkaTemplate.send(
                KafkaTopics.ORDER_CANCELED,
                event.data().orderId().toString(),
                event
        ).get();
    }
}