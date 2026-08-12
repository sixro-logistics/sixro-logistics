package com.sixro.logistics.hub.route.domain.strategy;

import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.model.PathSearchType;
import com.sixro.logistics.hub.route.domain.model.RouteNetworkEdge;

import java.util.Map;
import java.util.UUID;

public interface RoutingWeightStrategy {
    PathSearchType getSupportedType();
    double calculateWeight(RouteNetworkEdge routeNetworkEdge, Map<UUID, HubTransferMetric> metrics);
}