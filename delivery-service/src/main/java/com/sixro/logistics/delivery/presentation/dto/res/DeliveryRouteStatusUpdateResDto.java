package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliveryRouteStatusResult;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryRouteStatusUpdateResDto {

    private final UUID deliveryRouteId;
    private final UUID deliveryId;
    private final Integer routeSequence;
    private final RouteStatus routeStatus;
    private final DeliveryStatus deliveryStatus;
    private final UUID deliveryManagerId;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime updatedAt;

    public DeliveryRouteStatusUpdateResDto(DeliveryRouteStatusResult result) {
        this.deliveryRouteId = result.getDeliveryRouteId();
        this.deliveryId = result.getDeliveryId();
        this.routeSequence = result.getRouteSequence();
        this.routeStatus = result.getRouteStatus();
        this.deliveryStatus = result.getDeliveryStatus();
        this.deliveryManagerId = result.getDeliveryManagerId();
        this.startedAt = result.getStartedAt();
        this.completedAt = result.getCompletedAt();
        this.updatedAt = result.getUpdatedAt();
    }
}
