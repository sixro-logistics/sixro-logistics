package com.sixro.logistics.hub.route.domain.policy;

import com.sixro.logistics.hub.route.domain.model.HubRoute;

public interface RouteCostCalculationPolicy {

    int calculateCost(int totalDistanceMeters);
}