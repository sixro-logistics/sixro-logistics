package com.sixro.logistics.order.application.result;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderUpdateResult(
        UUID orderId,
        LocalDateTime deliveryDeadline,
        String requests
) {
}
