package com.sixro.logistics.inventory.application.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.inventory.domain.entity.outbox.AggregateType;
import com.sixro.logistics.inventory.domain.entity.outbox.EventType;
import com.sixro.logistics.inventory.domain.entity.outbox.Outbox;
import com.sixro.logistics.inventory.domain.event.InventoryDeductedEvent;
import com.sixro.logistics.inventory.domain.event.InventoryDeductionFailedEvent;
import com.sixro.logistics.inventory.domain.repository.outbox.OutboxRepository;
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

    public void save(InventoryDeductedEvent event) {
        save(
                event.orderId(),
                EventType.INVENTORY_DEDUCTED,
                event
        );
    }

    public void save(InventoryDeductionFailedEvent event) {
        save(
                event.orderId(),
                EventType.INVENTORY_DEDUCTION_FAILED,
                event
        );
    }

    private void save(
            UUID aggregateId,
            EventType eventType,
            Object event
    ) {
        try {
            String payload =
                    objectMapper.writeValueAsString(event); // 이벤트를 JSON 문자열로 만들어 Outbox에 저장

            Outbox outbox = Outbox.create(
                    aggregateId,
                    AggregateType.ORDER, // 이 이벤트가 어떤 Aggregate의 상태, 행동에 대한 사건인지
                    eventType,
                    payload
            );

            outboxRepository.save(outbox);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Outbox 직렬화에 실패했습니다.",
                    e
            );
        }
    }
}