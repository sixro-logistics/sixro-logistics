package com.sixro.logistics.order.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreateServiceCommand(

        UUID hubId,
        UUID ordererId,
        UUID receiverCompanyId,
        String deliveryAddress,
        LocalDateTime deliveryDeadline,
        String requests,
        List<OrderCreateServiceItem> orderItems

) {
}
