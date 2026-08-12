package com.sixro.logistics.hub.route.domain.strategy;

import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.model.PathSearchType;
import com.sixro.logistics.hub.route.domain.model.RouteNetworkEdge;
import com.sixro.logistics.hub.route.domain.policy.RouteCostCalculationPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OptimalWeightStrategy implements RoutingWeightStrategy {

    private final RouteCostCalculationPolicy costCalculationPolicy;

    // B2B 화물 운송 시간에 대한 기회비용 환산
    private static final double COST_PER_SECOND = 24000.0 / 3600.0; // 시간당 24,000원 (1초당 약 6.67원)

    @Override
    public PathSearchType getSupportedType() {
        return PathSearchType.OPTIMAL;
    }

    @Override
    public double calculateWeight(RouteNetworkEdge routeNetworkEdge, Map<UUID, HubTransferMetric> metrics) {
        // 재무적 비용
        double financialCost = costCalculationPolicy.calculatePathCost(routeNetworkEdge);

        // 시간적 비용
        double moveTimeSeconds = routeNetworkEdge.duration();
        HubTransferMetric destMetric = metrics.get(routeNetworkEdge.destinationHubId());
        // 환적시간 반영, 없으면 0 으로 계산
        double transferTimeSeconds = (destMetric != null) ? destMetric.getExpectedTransferTime() : 0.0;

        double totalTimeCost = (moveTimeSeconds + transferTimeSeconds) * COST_PER_SECOND;

        // 최종 임피던스
        return financialCost + totalTimeCost;
    }
}