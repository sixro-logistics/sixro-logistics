package com.sixro.logistics.delivery.application.command;

import lombok.Getter;

import java.util.UUID;

@Getter
public class GetDeliveryCommand {

    private final UUID deliveryId;
    private final UUID loginUserId;
    private final String userRole;
    private final UUID affiliationId;

    public GetDeliveryCommand(UUID deliveryId, UUID loginUserId, String userRole, UUID affiliationId) {
        this.deliveryId = deliveryId;
        this.loginUserId = loginUserId;
        this.userRole = userRole;
        this.affiliationId = affiliationId;
    }
}
