package com.sixro.logistics.hub.route.application.query;

import java.util.List;
import java.util.UUID;

public record OptimalPathResult(
        UUID originHubId,
        UUID destinationHubId,
        int totalDistance,
        int totalDuration,
        int totalCost,
        List<RouteSegment> segments
) {
    public record RouteSegment(
            UUID hubRouteId,
            int sequence,
            UUID originHubId,
            UUID destinationHubId,
            int distance,
            int duration,
            int cost
    ) {}
}