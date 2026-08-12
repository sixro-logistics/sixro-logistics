package com.sixro.logistics.hub.route.domain.repository;

import com.sixro.logistics.hub.route.domain.model.HubRoute;
import java.util.Optional;
import java.util.UUID;

public interface HubRouteCommandRepository {
    HubRoute save(HubRoute hubRoute);
    Optional<HubRoute> findById(UUID id);
    boolean existsByOriginHubIdAndDestinationHubId(UUID originHubId, UUID destinationHubId);
}