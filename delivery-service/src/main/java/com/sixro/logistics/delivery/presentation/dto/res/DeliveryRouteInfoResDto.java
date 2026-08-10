package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliveryRouteResult;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryRouteInfoResDto {

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

    public DeliveryRouteInfoResDto(DeliveryRouteResult result) {
        this.deliveryRouteId = result.getDeliveryRouteId();
        this.deliveryId = result.getDeliveryId();
        this.routeSequence = result.getRouteSequence();
        this.originHubId = result.getOriginHubId();
        this.destHubId = result.getDestHubId();
        this.expectedDistanceM = result.getExpectedDistanceM();
        this.expectedDurationS = result.getExpectedDurationS();
        this.routeStatus = result.getRouteStatus();
        this.deliveryManagerId = result.getDeliveryManagerId();
        this.startedAt = result.getStartedAt();
        this.completedAt = result.getCompletedAt();
        this.createdAt = result.getCreatedAt();
        this.updatedAt = result.getUpdatedAt();
    }
}
