package com.sixro.logistics.hub.route.domain.strategy;

import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.model.PathSearchType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class DistanceWeightStrategy implements RoutingWeightStrategy {

    @Override
    public PathSearchType getSupportedType() {
        return PathSearchType.DISTANCE;
    }

    @Override
    public double calculateWeight(HubRoute hubRoute, Map<UUID, HubTransferMetric> metrics) {
        return hubRoute.getDistance();
    }
}