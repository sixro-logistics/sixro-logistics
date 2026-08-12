package com.sixro.logistics.order.presentation.dto.response;

import com.sixro.logistics.order.application.result.OrderUpdateResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderUpdateResponseDto(
        UUID orderId,
        LocalDateTime deliveryDeadline,
        String requests
) {

    public static OrderUpdateResponseDto from(OrderUpdateResult result) {
        return new OrderUpdateResponseDto(
                result.orderId(),
                result.deliveryDeadline(),
                result.requests()
        );
    }
}
