package com.sixro.logistics.hub.hub.domain.model;

import java.util.UUID;

public record HubWithDistance(
        UUID hubId,
        String hubName,
        String hubStatus,
        Double distanceInMeters
) {}