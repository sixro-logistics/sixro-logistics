package com.sixro.logistics.order.application.command;

import java.util.UUID;

public record DeliveryCreationFailedCommand(
        UUID eventId,
        UUID orderId
) {
}
