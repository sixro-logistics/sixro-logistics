package com.sixro.logistics.delivery.application.model;

import java.util.List;
import java.util.UUID;

// Application이 이해하는 전체 허브 이동 경로
public record HubRoutePathInfo(
        UUID originHubId,
        UUID destHubId,
        List<RouteInfo> routes
) {
    public record RouteInfo(
            UUID hubRouteId,
            Integer routeSequence,
            UUID originHubId,
            UUID destHubId,
            Long expectedDistanceM,
            Long expectedDurationS
    ) {
    }
}
