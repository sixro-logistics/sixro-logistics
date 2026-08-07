package com.sixro.logistics.order.application.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.order.domain.entity.outbox.AggregateType;
import com.sixro.logistics.order.domain.entity.outbox.EventType;
import com.sixro.logistics.order.domain.entity.outbox.Outbox;
import com.sixro.logistics.order.domain.event.order.OrderCreatedEvent;
import com.sixro.logistics.order.domain.repository.outbox.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void save(OrderCreatedEvent event) {

        save(
                event.orderId(),
                EventType.ORDER_CREATED,
                event
        );
    }

    private void save(
            UUID aggregateId,
            EventType eventType,
            Object event
    ) {

        try {

            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.create(
                    aggregateId,
                    AggregateType.ORDER,
                    eventType,
                    payload
            );

            outboxRepository.save(outbox);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox 직렬화에 실패했습니다.", e);
        }
    }
}