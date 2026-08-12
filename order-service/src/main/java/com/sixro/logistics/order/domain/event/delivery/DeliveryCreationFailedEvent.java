package com.sixro.logistics.order.domain.event.delivery;

import java.util.UUID;

public record DeliveryCreationFailedEvent(
        UUID orderId
) {
}
