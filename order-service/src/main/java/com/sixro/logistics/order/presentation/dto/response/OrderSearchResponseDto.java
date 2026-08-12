package com.sixro.logistics.order.presentation.dto.response;

import com.sixro.logistics.order.domain.entity.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSearchResponseDto(
        UUID orderId,
        UUID receiverId,
        UUID hubId,
        UUID receiverCompanyId,
        String deliveryAddress,
        LocalDateTime deliveryDeadline,
        String requests,
        OrderStatus orderStatus
) {
}
