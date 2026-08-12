package com.sixro.logistics.hub.route.domain.strategy;

import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.model.PathSearchType;
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
    public double calculateWeight(HubRoute hubRoute, Map<UUID, HubTransferMetric> metrics) {

        double moveTimeSeconds = hubRoute.getDuration();
        HubTransferMetric destMetric = metrics.get(hubRoute.getDestinationHubId());
        // 환적시간 반영, 없으면 0 으로 계산
        double transferTimeSeconds = (destMetric != null) ? destMetric.getExpectedTransferTime() : 0.0;

        return moveTimeSeconds + transferTimeSeconds;
    }
}