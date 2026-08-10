package com.sixro.logistics.hub.infrastructure.persistence;

import java.util.UUID;

public interface NearestHubProjection {
    UUID getId();
    String getHubName();
    String getHubStatus();
    Double getDistanceInMeters();
}