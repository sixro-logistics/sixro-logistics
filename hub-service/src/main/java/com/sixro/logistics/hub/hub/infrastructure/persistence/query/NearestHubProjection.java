package com.sixro.logistics.hub.hub.infrastructure.persistence.query;

import java.util.UUID;

public interface NearestHubProjection {
    UUID getHubId();
    String getHubName();
    String getHubStatus();
    Double getDistanceInMeters();
}