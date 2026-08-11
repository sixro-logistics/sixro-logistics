package com.sixro.logistics.delivery.infrastructure.kafka.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxEventType;
import com.sixro.logistics.delivery.domain.port.OutboxRepositoryPort;
import com.sixro.logistics.delivery.application.event.DeliveryCreatedEvent;
import com.sixro.logistics.delivery.application.event.DeliveryCreationFailedEvent;
import com.sixro.logistics.delivery.infrastructure.kafka.producer.DeliveryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
                // Outbox 이벤트 복원 및 Kafka 발행
                publishEvent(outbox);

                // 발행 완료 처리
                outbox.publish(LocalDateTime.now());

                log.info("Outbox 발행 완료: eventId={}, aggregateId={}, eventType={}, traceId={}",
                        outbox.getEventId(), outbox.getAggregateId(), outbox.getEventType(), outbox.getTraceId());

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.error("Outbox 발행 중단: eventId={}, aggregateId={}, eventType={}, traceId={}",
                        outbox.getEventId(), outbox.getAggregateId(), outbox.getEventType(), outbox.getTraceId(), exception);
                return;

            } catch (Exception exception) {
                log.error("Outbox 발행 실패: eventId={}, aggregateId={}, eventType={}, traceId={}",
                        outbox.getEventId(), outbox.getAggregateId(), outbox.getEventType(), outbox.getTraceId(), exception);
            }
        }
    }

    private void publishEvent(Outbox outbox)
            throws JsonProcessingException, InterruptedException, ExecutionException {
        if (outbox.getEventType() == OutboxEventType.DELIVERY_CREATED) {
            DeliveryCreatedEvent event = objectMapper.readValue(outbox.getPayload(), DeliveryCreatedEvent.class);

            deliveryEventProducer.sendDeliveryCreatedEvent(event);
            return;
        }

        if (outbox.getEventType() == OutboxEventType.DELIVERY_CREATION_FAILED) {
            DeliveryCreationFailedEvent event =
                    objectMapper.readValue(outbox.getPayload(), DeliveryCreationFailedEvent.class);

            deliveryEventProducer.sendDeliveryCreationFailedEvent(event);
            return;
        }

        throw new IllegalArgumentException("지원하지 않는 Outbox 이벤트입니다: " + outbox.getEventType());
    }
}
