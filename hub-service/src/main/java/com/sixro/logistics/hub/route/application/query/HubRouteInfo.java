package com.sixro.logistics.hub.route.application.query;

import java.util.UUID;

public record HubRouteInfo(
        UUID hubRouteId,
        UUID originHubId,
        UUID destinationHubId,
        int distance,
        int duration,
        int tollFee,
        int baseCost
) {}