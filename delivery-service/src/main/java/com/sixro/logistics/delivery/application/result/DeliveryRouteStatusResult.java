package com.sixro.logistics.delivery.application.result;

import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryRouteStatusResult {

    private final UUID deliveryRouteId;
    private final UUID deliveryId;
    private final Integer routeSequence;
    private final RouteStatus routeStatus;
    private final DeliveryStatus deliveryStatus;
    private final UUID deliveryManagerId;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime updatedAt;

    public DeliveryRouteStatusResult(DeliveryRoute deliveryRoute) {
        this.deliveryRouteId = deliveryRoute.getDeliveryRouteId();
        this.deliveryId = deliveryRoute.getDelivery().getDeliveryId();
        this.routeSequence = deliveryRoute.getRouteSequence();
        this.routeStatus = deliveryRoute.getRouteStatus();
        this.deliveryStatus = deliveryRoute.getDelivery().getDeliveryStatus();
        this.deliveryManagerId = (deliveryRoute.getDeliveryManager() == null) ? null : deliveryRoute.getDeliveryManager().getDeliveryManagerId();
        this.startedAt = deliveryRoute.getStartedAt();
        this.completedAt = deliveryRoute.getCompletedAt();
        this.updatedAt = deliveryRoute.getUpdatedAt();
    }
}
