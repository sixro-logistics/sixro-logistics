package com.sixro.logistics.hub.infrastructure.persistence;

import java.util.UUID;

public interface NearestHubProjection {
    UUID getHubId();
    String getHubName();
    String getHubStatus();
    Double getDistanceInMeters();
}