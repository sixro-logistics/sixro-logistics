package com.sixro.logistics.order.application.command;

import java.util.UUID;

public record DeliveryCreatedCommand(
        UUID eventId,
        UUID orderId,
        UUID deliveryId
) {
}
