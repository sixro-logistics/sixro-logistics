package com.sixro.logistics.inventory.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.inventory.application.command.InventoryCommandItem;
import com.sixro.logistics.inventory.application.command.InventoryRestoreByEventCommand;
import com.sixro.logistics.inventory.application.facade.InventoryCommandFacade;
import com.sixro.logistics.inventory.domain.event.OrderCanceledEvent;
import com.sixro.logistics.inventory.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final InventoryCommandFacade inventoryCommandFacade;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.ORDER_CANCELED,
            groupId = "inventory-service"
    )
    public void consume(String message) {

        try {
            EventEnvelope<OrderCanceledEvent> event =
                    objectMapper.readValue(
                            message,
                            new TypeReference<EventEnvelope<OrderCanceledEvent>>() {}
                    );

            inventoryCommandFacade.restoreInventoryByEvent(
                    new InventoryRestoreByEventCommand(
                            event.eventId(),
                            event.data().orderId(),
                            event.data().hubId(),
                            event.data().items()
                                    .stream()
                                    .map(item -> new InventoryCommandItem(
                                            item.productId(),
                                            item.quantity()
                                    ))
                                    .toList()
                    )
            );

        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "ORDER_CANCELED 이벤트 역직렬화에 실패했습니다.",
                    e
            );
        }
    }
}
