package com.sixro.logistics.delivery.application.command;

import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Getter
public class SearchDeliveryRoutesCommand {

    private final UUID loginUserId;
    private final String userRole;
    private final UUID affiliationId;
    private final UUID deliveryId;
    private final RouteStatus routeStatus;
    private final UUID originHubId;
    private final UUID destHubId;
    private final UUID deliveryManagerId;
    private final Integer routeSequence;
    private final Pageable pageable;

    public SearchDeliveryRoutesCommand(UUID loginUserId, String userRole, UUID affiliationId, UUID deliveryId,
                                       RouteStatus routeStatus, UUID originHubId, UUID destHubId,
                                       UUID deliveryManagerId, Integer routeSequence, Pageable pageable) {
        this.loginUserId = loginUserId;
        this.userRole = userRole;
        this.affiliationId = affiliationId;
        this.deliveryId = deliveryId;
        this.routeStatus = routeStatus;
        this.originHubId = originHubId;
        this.destHubId = destHubId;
        this.deliveryManagerId = deliveryManagerId;
        this.routeSequence = routeSequence;
        this.pageable = pageable;
    }
}
