package com.sixro.logistics.hub.route.domain.policy;

import com.sixro.logistics.hub.route.domain.model.RouteNetworkEdge;

public interface RouteCostCalculationPolicy {
    int calculateBaseCost(int totalDistanceMeters);
    int calculatePathCost(RouteNetworkEdge hubRouteNetworkEdgeRoute);
}