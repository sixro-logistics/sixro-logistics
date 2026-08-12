package com.sixro.logistics.order.application.model;

import java.util.List;
import java.util.UUID;

public record DeliveryManagerInfo(
        List<UUID> deliveryManagerIds
) {
}
