package com.sixro.logistics.delivery.infrastructure.client.hubroute;

import java.util.List;
import java.util.UUID;

public record HubRouteClientResponse(
        UUID originHubId,
        UUID destHubId,
        List<Route> routes
) {
    public record Route(
            UUID hubRouteId,
            Integer sequence,
            UUID originHubId,
            UUID destHubId,
            Long expectedDistanceM,
            Long expectedDurationS
    ) {
    }
}
