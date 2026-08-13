package com.sixro.logistics.order.application.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.order.domain.entity.outbox.Outbox;
import com.sixro.logistics.order.domain.event.order.OrderCanceledEvent;
import com.sixro.logistics.order.domain.event.order.OrderCreatedEvent;
import com.sixro.logistics.order.domain.event.order.OrderFailedEvent;
import com.sixro.logistics.order.domain.repository.outbox.OutboxRepository;
import com.sixro.logistics.order.infrastructure.kafka.KafkaProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000) // 3초마다 조회
    @Transactional
    public void publish() {

        List<Outbox> outboxes =
                outboxRepository.findPendingOutboxes();

        if (outboxes.isEmpty()) {
            return;
        }

        log.info("발행 대기 Outbox {}건 조회", outboxes.size());

        for (Outbox outbox : outboxes) {
            try {
                log.info(
                        "Outbox 발행 시작. outboxId={}, eventType={}",
                        outbox.getId(),
                        outbox.getEventType()
                );

                switch (outbox.getEventType()) {

                    case ORDER_CREATED -> {

                        String payload = outbox.getPayload();

                        EventEnvelope<OrderCreatedEvent> event =
                                objectMapper.readValue(
                                        payload,
                                        new TypeReference<EventEnvelope<OrderCreatedEvent>>() {}
                                );

                        kafkaProducer.sendOrderCreatedEvent(event);
                    }

                    case ORDER_CANCELED -> {

                        String payload = outbox.getPayload();

                        EventEnvelope<OrderCanceledEvent> event =
                                objectMapper.readValue(
                                        payload,
                                        new TypeReference<EventEnvelope<OrderCanceledEvent>>() {}
                                );

                        kafkaProducer.sendOrderCanceledEvent(event);
                    }

                    case ORDER_FAILED -> {

                        String payload = outbox.getPayload();

                        EventEnvelope<OrderFailedEvent> event =
                                objectMapper.readValue(
                                        payload,
                                        new TypeReference<EventEnvelope<OrderFailedEvent>>() {}
                                );

                        kafkaProducer.sendOrderFailedEvent(event);
                    }

                    default -> throw new IllegalArgumentException(
                            "지원하지 않는 EventType : "
                                    + outbox.getEventType()
                    );
                }
                outbox.publish();

                log.info(
                        "Outbox 발행 완료. outboxId={}",
                        outbox.getId()
                );
            } catch (Exception e) {
                log.error(
                        "Outbox 발행 실패. outboxId={}",
                        outbox.getId(),
                        e
                );
            }
        }
    }
}