package com.sixro.logistics.hub.route.domain.strategy;

import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.model.PathSearchType;
import com.sixro.logistics.hub.route.domain.model.RouteNetworkEdge;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class DurationWeightStrategy implements RoutingWeightStrategy {

    @Override
    public PathSearchType getSupportedType() {
        return PathSearchType.DURATION;
    }

    @Override
    public double calculateWeight(RouteNetworkEdge routeNetworkEdge, Map<UUID, HubTransferMetric> metrics) {

        double moveTimeSeconds = routeNetworkEdge.duration();
        HubTransferMetric destMetric = metrics.get(routeNetworkEdge.destinationHubId());
        // 환적시간 반영, 없으면 0 으로 계산
        double transferTimeSeconds = (destMetric != null) ? destMetric.getExpectedTransferTime() : 0.0;

        return moveTimeSeconds + transferTimeSeconds;
    }
}