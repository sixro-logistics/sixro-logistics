package com.sixro.logistics.hub.route.presentation.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sixro.logistics.hub.route.application.query.OptimalPathResult;
import java.util.List;
import java.util.UUID;

public record OptimalPathResponse(
        UUID originHubId,
        @JsonProperty("destHubId")
        UUID destinationHubId,
        int totalDistanceM,
        int totalDurationS,
        int totalCostWon,
        List<RouteSegmentResponse> routes
) {
    public record RouteSegmentResponse(
            UUID hubRouteId,
            int sequence,
            UUID originHubId,
            @JsonProperty("destHubId")
            UUID destinationHubId,
            int expectedDistanceM,
            int expectedDurationS,
            int expectedCostWon
    ) {}

    public static OptimalPathResponse from(OptimalPathResult result) {
        List<RouteSegmentResponse> segments = result.segments().stream()
                .map(s -> new RouteSegmentResponse(
                        s.hubRouteId(),
                        s.sequence(),
                        s.originHubId(),
                        s.destinationHubId(),
                        s.distance(),
                        s.duration(),
                        s.cost()
                )).toList();

        return new OptimalPathResponse(
                result.originHubId(),
                result.destinationHubId(),
                result.totalDistance(),
                result.totalDuration(),
                result.totalCost(),
                segments
        );
    }
}