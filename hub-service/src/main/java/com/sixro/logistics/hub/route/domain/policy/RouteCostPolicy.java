package com.sixro.logistics.hub.route.domain.policy;

import com.sixro.logistics.hub.route.domain.model.RouteNetworkEdge;
import org.springframework.stereotype.Component;

@Component
public class RouteCostPolicy implements RouteCostCalculationPolicy {

    private static final int FIXED_BASE_COST = 50_000;      // C_base: 기본 배차 고정비
    private static final int COST_PER_KM = 1_500;           // U_km: km당 표준 단가
    private static final double COST_PER_METER_FUEL = 0.4;  // C_fuel: 미터당 유류비 (400원/km)

    @Override
    public int calculateBaseCost(int totalDistanceMeters) {
        if (totalDistanceMeters <= 0) {
            return FIXED_BASE_COST;
        }

        // 미터(m) 단위를 킬로미터(km)로 변환
        double distanceKm = totalDistanceMeters / 1000.0;

        // 변동비 계산
        int variableCost = (int) Math.round(distanceKm * COST_PER_KM);

        return FIXED_BASE_COST + variableCost;
    }

    @Override
    public int calculatePathCost(RouteNetworkEdge hubRouteNetworkEdgeRoute) {
        int baseCost = hubRouteNetworkEdgeRoute.getRouteCost().getBaseCost();
        int tollFee = hubRouteNetworkEdgeRoute.getRouteCost().getTollFee();
        int fuelCost = (int) Math.round(hubRouteNetworkEdgeRoute.distance() * COST_PER_METER_FUEL);

        return baseCost + tollFee + fuelCost;
    }
}