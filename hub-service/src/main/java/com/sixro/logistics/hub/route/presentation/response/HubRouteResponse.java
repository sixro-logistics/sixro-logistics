package com.sixro.logistics.hub.route.presentation.response;

import com.sixro.logistics.hub.route.application.query.HubRouteInfo;

import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteResponse(
        UUID hubRouteId,
        UUID originHubId,
        String originHubName,
        UUID destinationHubId,
        String destinationHubName,
        int distance,
        int duration,
        int baseCost,
        int tollFee,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HubRouteResponse from(HubRouteInfo info) {
        return new HubRouteResponse(
                info.hubRouteId(),
                info.originHubId(),
                info.originHubName(),
                info.destinationHubId(),
                info.destinationHubName(),
                info.distance(),
                info.duration(),
                info.baseCost(),
                info.tollFee(),
                info.createdAt(),
                info.updatedAt()
        );
    }
}