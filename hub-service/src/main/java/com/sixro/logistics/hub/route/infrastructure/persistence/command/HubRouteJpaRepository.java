package com.sixro.logistics.hub.route.infrastructure.persistence.command;

import com.sixro.logistics.hub.route.domain.model.HubRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface HubRouteJpaRepository extends JpaRepository<HubRoute, UUID> {
    boolean existsByOriginHubIdAndDestinationHubId(UUID originHubId, UUID destinationHubId);
}