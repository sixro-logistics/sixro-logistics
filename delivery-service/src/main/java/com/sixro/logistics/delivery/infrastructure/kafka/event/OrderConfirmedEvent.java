package com.sixro.logistics.delivery.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID orderId,
        UUID hubId,
        UUID receiverCompanyId,
        UUID receiverId,
        String deliveryAddress,
        LocalDateTime deliveryDeadline,
        String requests,
        List<OrderConfirmedItem> orderItems
) {
}
