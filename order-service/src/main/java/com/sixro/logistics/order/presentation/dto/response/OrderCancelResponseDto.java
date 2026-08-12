package com.sixro.logistics.order.presentation.dto.response;

import com.sixro.logistics.order.application.result.OrderCancelResult;
import com.sixro.logistics.order.domain.entity.order.OrderStatus;

import java.util.UUID;

public record OrderCancelResponseDto(
        UUID orderId,
        OrderStatus orderStatus
) {

    public static OrderCancelResponseDto from(OrderCancelResult result){
        return new OrderCancelResponseDto(
                result.orderId(),
                result.orderStatus()
        );
    }

}
