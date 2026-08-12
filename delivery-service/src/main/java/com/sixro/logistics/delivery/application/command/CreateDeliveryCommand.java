package com.sixro.logistics.delivery.application.command;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class CreateDeliveryCommand {

    private final String traceId;
    private final UUID orderId;
    private final UUID originHubId;
    private final UUID receiverCompanyId;
    private final UUID receiverId;
    private final String deliveryAddress;
    private final LocalDateTime deliveryDeadline;
    private final String requests;
    private final List<CreateDeliveryItemCommand> orderItems;

    public CreateDeliveryCommand(String traceId, UUID orderId, UUID originHubId, UUID receiverCompanyId, UUID receiverId,
                                 String deliveryAddress, LocalDateTime deliveryDeadline, String requests,
                                 List<CreateDeliveryItemCommand> orderItems) {
        this.traceId = traceId;
        this.orderId = orderId;
        this.originHubId = originHubId;
        this.receiverCompanyId = receiverCompanyId;
        this.receiverId = receiverId;
        this.deliveryAddress = deliveryAddress;
        this.deliveryDeadline = deliveryDeadline;
        this.requests = requests;
        this.orderItems = orderItems;
    }
}
