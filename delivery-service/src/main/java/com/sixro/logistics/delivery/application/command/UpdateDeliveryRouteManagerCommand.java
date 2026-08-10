package com.sixro.logistics.delivery.application.command;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UpdateDeliveryRouteManagerCommand {

    private final UUID deliveryRouteId;
    private final UUID deliveryManagerId;
    private final UUID loginUserId;
    private final String userRole;
    private final UUID affiliationId;

    public UpdateDeliveryRouteManagerCommand(UUID deliveryRouteId, UUID deliveryManagerId, UUID loginUserId, String userRole, UUID affiliationId) {
        this.deliveryRouteId = deliveryRouteId;
        this.deliveryManagerId = deliveryManagerId;
        this.loginUserId = loginUserId;
        this.userRole = userRole;
        this.affiliationId = affiliationId;
    }
}
