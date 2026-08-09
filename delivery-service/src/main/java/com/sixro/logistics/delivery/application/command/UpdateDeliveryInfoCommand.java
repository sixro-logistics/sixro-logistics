package com.sixro.logistics.delivery.application.command;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class UpdateDeliveryInfoCommand {

    private final UUID deliveryId;
    private final String deliveryAddress;
    private final LocalDateTime deliveryDeadline;
    private final String requests;
    private final String recipientName;
    private final String recipientSlackId;
    private final UUID loginUserId;
    private final String userRole;
    private final UUID affiliationId;

    public UpdateDeliveryInfoCommand(UUID deliveryId, String deliveryAddress, LocalDateTime deliveryDeadline,
                                     String requests, String recipientName, String recipientSlackId,
                                     UUID loginUserId, String userRole, UUID affiliationId) {
        this.deliveryId = deliveryId;
        this.deliveryAddress = deliveryAddress;
        this.deliveryDeadline = deliveryDeadline;
        this.requests = requests;
        this.recipientName = recipientName;
        this.recipientSlackId = recipientSlackId;
        this.loginUserId = loginUserId;
        this.userRole = userRole;
        this.affiliationId = affiliationId;
    }
}
