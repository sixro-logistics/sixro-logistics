package com.sixro.logistics.hub.route.presentation.response;

import com.sixro.logistics.hub.route.application.query.HubRouteInfo;
import java.util.UUID;

// 목록 조회 응답용 DTO
public record HubRouteResponse(
        UUID hubRouteId,
        UUID originHubId,
        UUID destinationHubId,
        int distance,
        int duration
) {
    public static HubRouteResponse from(HubRouteInfo info) {
        return new HubRouteResponse(
                info.hubRouteId(),
                info.originHubId(),
                info.destinationHubId(),
                info.distance(),
                info.duration()
        );
    }
}