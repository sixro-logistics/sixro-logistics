package com.sixro.logistics.delivery.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderConfirmedData(
        UUID orderId,
        UUID receiverId,
        UUID hubId,
        UUID receiverCompanyId,
        String deliveryAddress,
        LocalDateTime deliveryDeadline,
        String requests,
        List<OrderConfirmedItem> items
) {
}
