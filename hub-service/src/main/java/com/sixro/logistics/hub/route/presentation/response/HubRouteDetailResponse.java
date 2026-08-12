package com.sixro.logistics.hub.route.presentation.response;

import com.sixro.logistics.hub.route.application.query.GeoJsonLineString;
import com.sixro.logistics.hub.route.application.query.HubRouteDetailInfo;

import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteDetailResponse(
        UUID hubRouteId,
        UUID originHubId,
        String originHubName,
        UUID destinationHubId,
        String destinationHubName,
        int distance,
        int duration,
        int baseCost,
        int tollFee,
        GeoJsonLineString routePath,
        LocalDateTime createdAt,
        UUID createdBy,
        LocalDateTime updatedAt,
        UUID updatedBy
) {
    public static HubRouteDetailResponse from(HubRouteDetailInfo info) {
        return new HubRouteDetailResponse(
                info.hubRouteId(),
                info.originHubId(),
                info.originHubName(),
                info.destinationHubId(),
                info.destinationHubName(),
                info.distance(),
                info.duration(),
                info.baseCost(),
                info.tollFee(),
                info.routePath(),
                info.createdAt(),
                info.createdBy(),
                info.updatedAt(),
                info.updatedBy()
        );
    }
}