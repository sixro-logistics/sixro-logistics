package com.sixro.logistics.order.domain.event.order;

import java.util.List;
import java.util.UUID;

// 도메인 이벤트
public record OrderCreatedEvent(
        UUID orderId,
        UUID hubId,
        List<OrderCreatedItem> items

) {
}