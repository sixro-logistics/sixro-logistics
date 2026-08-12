package com.sixro.logistics.order.presentation.dto.response;

import com.sixro.logistics.order.application.result.OrderDeleteResult;
import com.sixro.logistics.order.domain.entity.order.OrderStatus;

import java.util.UUID;

public record OrderDeleteResponseDto(
        UUID orderId,
        OrderStatus orderStatus
) {
    public static OrderDeleteResponseDto from(OrderDeleteResult result) {
        return new OrderDeleteResponseDto(
                result.orderId(),
                result.orderStatus()
        );
    }
}
