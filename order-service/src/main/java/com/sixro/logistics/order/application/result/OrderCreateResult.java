package com.sixro.logistics.order.application.result;

import com.sixro.logistics.order.domain.entity.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreateResult(
        UUID orderId,
        UUID hubId,
        UUID receiverCompanyId,
        String deliveryAddress,
        LocalDateTime deliveryDeadline,
        String requests,
        OrderStatus orderStatus,
        List<OrderResultItem> orderItems
) {

}
