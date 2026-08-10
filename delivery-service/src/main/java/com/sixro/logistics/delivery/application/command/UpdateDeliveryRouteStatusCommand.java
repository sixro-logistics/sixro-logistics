package com.sixro.logistics.delivery.application.command;

import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UpdateDeliveryRouteStatusCommand {

    private final UUID deliveryRouteId;
    private final RouteStatus routeStatus;
    private final UUID loginUserId;
    private final String userRole;
    private final UUID affiliationId;

    public UpdateDeliveryRouteStatusCommand(UUID deliveryRouteId, RouteStatus routeStatus, UUID loginUserId, String userRole, UUID affiliationId) {
        this.deliveryRouteId = deliveryRouteId;
        this.routeStatus = routeStatus;
        this.loginUserId = loginUserId;
        this.userRole = userRole;
        this.affiliationId = affiliationId;
    }
}
