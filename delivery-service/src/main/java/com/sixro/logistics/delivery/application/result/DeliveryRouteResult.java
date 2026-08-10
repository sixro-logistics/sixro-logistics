package com.sixro.logistics.delivery.application.result;

import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryRouteResult {

    private final UUID deliveryRouteId;
    private final UUID deliveryId;
    private final Integer routeSequence;
    private final UUID originHubId;
    private final UUID destHubId;
    private final Long expectedDistanceM;
    private final Long expectedDurationS;
    private final RouteStatus routeStatus;
    private final UUID deliveryManagerId;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public DeliveryRouteResult(DeliveryRoute deliveryRoute) {
        this.deliveryRouteId = deliveryRoute.getDeliveryRouteId();
        this.deliveryId = deliveryRoute.getDelivery().getDeliveryId();
        this.routeSequence = deliveryRoute.getRouteSequence();
        this.originHubId = deliveryRoute.getOriginHubId();
        this.destHubId = deliveryRoute.getDestHubId();
        this.expectedDistanceM = deliveryRoute.getExpectedDistanceM();
        this.expectedDurationS = deliveryRoute.getExpectedDurationS();
        this.routeStatus = deliveryRoute.getRouteStatus();
        this.deliveryManagerId = (deliveryRoute.getDeliveryManager() == null) ? null : deliveryRoute.getDeliveryManager().getDeliveryManagerId();
        this.startedAt = deliveryRoute.getStartedAt();
        this.completedAt = deliveryRoute.getCompletedAt();
        this.createdAt = deliveryRoute.getCreatedAt();
        this.updatedAt = deliveryRoute.getUpdatedAt();
    }
}
