package com.sixro.logistics.delivery.application.command;

import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UpdateDeliveryStatusCommand {

    private final UUID deliveryId;
    private final DeliveryStatus deliveryStatus;
    private final UUID loginUserId;
    private final String userRole;
    private final UUID affiliationId;

    public UpdateDeliveryStatusCommand(UUID deliveryId, DeliveryStatus deliveryStatus, UUID loginUserId, String userRole, UUID affiliationId) {
        this.deliveryId = deliveryId;
        this.deliveryStatus = deliveryStatus;
        this.loginUserId = loginUserId;
        this.userRole = userRole;
        this.affiliationId = affiliationId;
    }
}
