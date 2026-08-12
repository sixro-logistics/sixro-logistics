package com.sixro.logistics.order.application.result;

import com.sixro.logistics.order.domain.entity.order.OrderStatus;

import java.util.UUID;

public record OrderCancelResult(
        UUID orderId,
        OrderStatus orderStatus
) {
}
