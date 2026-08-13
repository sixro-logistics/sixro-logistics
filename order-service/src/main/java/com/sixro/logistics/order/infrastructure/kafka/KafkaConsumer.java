package com.sixro.logistics.order.infrastructure.kafka;

import com.sixro.logistics.order.application.command.DeliveryCreatedCommand;
import com.sixro.logistics.order.application.command.DeliveryCreationFailedCommand;
import com.sixro.logistics.order.application.facade.order.OrderCommandFacade;
import com.sixro.logistics.order.application.event.DeliveryCreatedEvent;
import com.sixro.logistics.order.application.event.DeliveryCreationFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final OrderCommandFacade orderCommandFacade;

    @KafkaListener(topics = KafkaTopics.DELIVERY_CREATED)
    public void consumeDeliveryCreated(
            DeliveryCreatedEvent event
    ) {

        orderCommandFacade.deliveryCreated(
                new DeliveryCreatedCommand(
                        event.eventId(),
                        event.data().orderId(),
                        event.data().deliveryId()
                )
        );

    }

    @KafkaListener(topics = KafkaTopics.DELIVERY_CREATION_FAILED)
    public void consumeDeliveryCreationFailed(
            DeliveryCreationFailedEvent event
    ) {
        orderCommandFacade.deliveryCreationFailed(
                new DeliveryCreationFailedCommand(
                        event.eventId(),
                        event.data().orderId()
                )
        );
    }

}