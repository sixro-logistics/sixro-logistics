package com.sixro.logistics.order.presentation.dto.response;

import com.sixro.logistics.order.application.result.OrderGetOneResult;
import com.sixro.logistics.order.domain.entity.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderGetOneResponseDto(
        UUID orderId,
        UUID receiverId,
        UUID hubId,
        UUID receiverCompanyId,
        String deliveryAddress,
        LocalDateTime deliveryDeadline,
        String requests,
        OrderStatus orderStatus,
        List<OrderResponseItem> orderItems
) {

    public static OrderGetOneResponseDto from(OrderGetOneResult result) {
        return new OrderGetOneResponseDto(
                result.orderId(),
                result.receiverId(),
                result.hubId(),
                result.receiverCompanyId(),
                result.deliveryAddress(),
                result.deliveryDeadline(),
                result.requests(),
                result.orderStatus(),
                result.orderItems().stream()
                        .map(item -> new OrderResponseItem(
                                item.productId(),
                                item.productName(),
                                item.productPrice(),
                                item.quantity(),
                                item.companyId()
                        ))
                        .toList()
        );
    }

}
