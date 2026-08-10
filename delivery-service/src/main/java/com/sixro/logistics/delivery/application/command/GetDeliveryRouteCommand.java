package com.sixro.logistics.delivery.application.command;

import lombok.Getter;

import java.util.UUID;

@Getter
public class GetDeliveryRouteCommand {

    private final UUID deliveryRouteId;
    private final UUID loginUserId;
    private final String userRole;
    private final UUID affiliationId;

    public GetDeliveryRouteCommand(UUID deliveryRouteId, UUID loginUserId, String userRole, UUID affiliationId) {
        this.deliveryRouteId = deliveryRouteId;
        this.loginUserId = loginUserId;
        this.userRole = userRole;
        this.affiliationId = affiliationId;
    }
}
