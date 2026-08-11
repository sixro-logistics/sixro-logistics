package com.sixro.logistics.hub.route.application.command;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.model.RouteCost;
import com.sixro.logistics.hub.route.domain.repository.HubRouteCommandRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class HubRouteStore {

    private final HubRouteCommandRepository repository;

    public UUID save(HubRoute hubRoute) {
        return repository.save(hubRoute).getId();
    }

    public UUID updateCostAndPath(UUID routeId, int distance, int duration, RouteCost routeCost, LineString routePath) {
        HubRoute hubRoute = getRouteOrThrow(routeId);
        hubRoute.update(distance, duration, routeCost, routePath);
        return hubRoute.getId();
    }

    public void delete(UUID routeId, UUID deletedBy) {
        HubRoute hubRoute = getRouteOrThrow(routeId);
        hubRoute.softDelete(deletedBy);
    }

    private HubRoute getRouteOrThrow(UUID routeId) {
        return repository.findById(routeId)
                .orElseThrow(() -> new BaseException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND));
    }
}