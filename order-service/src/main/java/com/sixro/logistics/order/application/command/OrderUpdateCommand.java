package com.sixro.logistics.order.application.command;

import java.time.LocalDateTime;

public record OrderUpdateCommand(
        LocalDateTime deliveryDeadline,
        String requests
) {
}
