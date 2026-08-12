package com.sixro.logistics.hub.route.domain.repository;

import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.model.RouteNetworkEdge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRouteQueryRepository {
    Optional<HubRoute> findById(UUID id);
    Page<HubRoute> search(UUID originHubId, UUID destinationHubId, Pageable pageable);
    List<RouteNetworkEdge> findAllOperatingRouteEdges();
}