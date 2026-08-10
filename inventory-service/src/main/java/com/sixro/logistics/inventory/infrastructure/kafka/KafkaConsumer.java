package com.sixro.logistics.inventory.infrastructure.kafka;

import com.sixro.logistics.inventory.domain.event.OrderCreatedEvent;
import com.sixro.logistics.inventory.application.facade.InventoryCommandFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final InventoryCommandFacade inventoryCommandFacade;

    @KafkaListener(
            topics = "order-created",
            groupId = "inventory-service"
    )
    public void consume(OrderCreatedEvent event) {

        inventoryCommandFacade.deductStock(
                event.orderId(),
                event.hubId(),
                event.items()
        );

    }

}
