package com.sixro.logistics.order.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreateCommand(

        UUID hubId,
        UUID receiverCompanyId,
        LocalDateTime deliveryDeadline,
        String requests,
        List<OrderCommandItem> orderItems

) {
}
