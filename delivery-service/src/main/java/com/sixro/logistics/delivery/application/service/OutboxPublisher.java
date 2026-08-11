package com.sixro.logistics.delivery.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxEventType;
import com.sixro.logistics.delivery.domain.port.OutboxRepositoryPort;
import com.sixro.logistics.delivery.infrastructure.kafka.event.DeliveryCreatedEvent;
import com.sixro.logistics.delivery.infrastructure.kafka.producer.DeliveryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OutboxPublisher {

    private final OutboxRepositoryPort outboxRepositoryPort;
    private final DeliveryEventProducer deliveryEventProducer;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxRepositoryPort outboxRepositoryPort,
                           DeliveryEventProducer deliveryEventProducer,
                           ObjectMapper objectMapper) {
        this.outboxRepositoryPort = outboxRepositoryPort;
        this.deliveryEventProducer = deliveryEventProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publish() {
        // 발행 대상 조회
        List<Outbox> outboxes = outboxRepositoryPort.findPendingOutboxes();
        if (outboxes.isEmpty()) {
            return;
        }

        log.info("발행 대기 Outbox 조회: count={}", outboxes.size());

        for (Outbox outbox : outboxes) {
            try {
                // Outbox 이벤트 복원
                DeliveryCreatedEvent event = createDeliveryCreatedEvent(outbox);

                // Kafka 이벤트 발행
                deliveryEventProducer.sendDeliveryCreatedEvent(event, outbox.getTraceId());

                // 발행 완료 처리
                outbox.publish(LocalDateTime.now());

                log.info("Outbox 발행 완료: eventId={}, deliveryId={}, traceId={}",
                        outbox.getEventId(), outbox.getAggregateId(), outbox.getTraceId());

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.error("Outbox 발행 중단: eventId={}, deliveryId={}, traceId={}",
                        outbox.getEventId(), outbox.getAggregateId(), outbox.getTraceId(), exception);
                return;

            } catch (Exception exception) {
                log.error("Outbox 발행 실패: eventId={}, deliveryId={}, traceId={}",
                        outbox.getEventId(), outbox.getAggregateId(), outbox.getTraceId(), exception);
            }
        }
    }

    private DeliveryCreatedEvent createDeliveryCreatedEvent(Outbox outbox) throws JsonProcessingException {
        if (outbox.getEventType() != OutboxEventType.DELIVERY_CREATED) {
            throw new IllegalArgumentException("지원하지 않는 Outbox 이벤트입니다: " + outbox.getEventType());
        }

        return objectMapper.readValue(outbox.getPayload(), DeliveryCreatedEvent.class);
    }
}
