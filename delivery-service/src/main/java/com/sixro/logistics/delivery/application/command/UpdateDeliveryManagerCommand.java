package com.sixro.logistics.delivery.application.command;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UpdateDeliveryManagerCommand {

    private final UUID deliveryId;
    private final UUID deliveryManagerId;
    private final UUID loginUserId;
    private final String userRole;
    private final UUID affiliationId;

    public UpdateDeliveryManagerCommand(UUID deliveryId, UUID deliveryManagerId, UUID loginUserId, String userRole, UUID affiliationId) {
        this.deliveryId = deliveryId;
        this.deliveryManagerId = deliveryManagerId;
        this.loginUserId = loginUserId;
        this.userRole = userRole;
        this.affiliationId = affiliationId;
    }
}
