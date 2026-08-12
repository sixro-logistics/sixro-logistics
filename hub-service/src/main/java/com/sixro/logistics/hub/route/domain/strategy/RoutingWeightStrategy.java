package com.sixro.logistics.hub.route.domain.strategy;

import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.model.PathSearchType;

import java.util.Map;
import java.util.UUID;

public interface RoutingWeightStrategy {
    PathSearchType getSupportedType();
    double calculateWeight(HubRoute hubRoute, Map<UUID, HubTransferMetric> metrics);
}