package com.sixro.logistics.hub.route.application.query;

import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteInfo(
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
) {}