package com.sixro.logistics.order.infrastructure.client.delivery;

import java.util.List;
import java.util.UUID;

public record DeliveryClientResponse(
        List<UUID> deliveryManagerIds
) {
}
