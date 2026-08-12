package com.sixro.logistics.delivery.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxEventType;
import com.sixro.logistics.delivery.domain.port.OutboxRepositoryPort;
import com.sixro.logistics.delivery.application.event.DeliveryCreatedEvent;
import com.sixro.logistics.delivery.application.event.DeliveryCreationFailedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OutboxService {

    private final OutboxRepositoryPort outboxRepositoryPort;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepositoryPort outboxRepositoryPort, ObjectMapper objectMapper) {
        this.outboxRepositoryPort = outboxRepositoryPort;
        this.objectMapper = objectMapper;
    }

    public void save(DeliveryCreatedEvent event, String traceId) {
        try {
            // 이벤트 JSON
            String payload = objectMapper.writeValueAsString(event);

            // Outbox 이벤트 구성
            Outbox outbox = Outbox.create(
                    event.eventId(), event.data().deliveryId(), OutboxEventType.DELIVERY_CREATED,
                    traceId, payload, event.occurredAt()
            );

            // Outbox 이벤트 저장
            outboxRepositoryPort.save(outbox);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("DeliveryCreatedEvent 이벤트 JSON 구성에 실패했습니다.", exception);
        }
    }

    public void save(DeliveryCreationFailedEvent event, String traceId) {
        try {
            // 실패 이벤트 JSON
            String payload = objectMapper.writeValueAsString(event);

            // 실패 Outbox 이벤트 구성
            Outbox outbox = Outbox.create(
                    event.eventId(), event.data().orderId(), OutboxEventType.DELIVERY_CREATION_FAILED,
                    traceId, payload, event.occurredAt()
            );

            // 실패 Outbox 이벤트 저장
            outboxRepositoryPort.save(outbox);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("DeliveryCreationFailedEvent 이벤트 JSON 구성에 실패했습니다.", exception);
        }
    }
}
