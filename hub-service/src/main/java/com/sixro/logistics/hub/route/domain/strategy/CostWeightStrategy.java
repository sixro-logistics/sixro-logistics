package com.sixro.logistics.hub.route.domain.strategy;

import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.model.PathSearchType;
import com.sixro.logistics.hub.route.domain.policy.RouteCostCalculationPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CostWeightStrategy implements RoutingWeightStrategy {

    private final RouteCostCalculationPolicy costCalculationPolicy;

    @Override
    public PathSearchType getSupportedType() {
        return PathSearchType.COST;
    }

    @Override
    public double calculateWeight(HubRoute hubRoute, Map<UUID, HubTransferMetric> metrics) {
        return costCalculationPolicy.calculatePathCost(hubRoute);
    }
}