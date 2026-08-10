package com.sixro.logistics.hub.route.domain.policy;

import org.springframework.stereotype.Component;

@Component
public class StandardBaseCostPolicy implements RouteCostCalculationPolicy {

    // TODO: .yml 또는 설정 DB 처리로 분리
    private static final int FIXED_BASE_COST = 50_000; // C_base: 기본 배차 고정비
    private static final int COST_PER_KM = 1_500;      // U_km: km당 표준 단가

    @Override
    public int calculateCost(int totalDistanceMeters) {
        if (totalDistanceMeters <= 0) {
            return FIXED_BASE_COST;
        }

        // 미터(m) 단위를 킬로미터(km)로 변환
        double distanceKm = totalDistanceMeters / 1000.0;

        // 변동비 계산
        int variableCost = (int) Math.round(distanceKm * COST_PER_KM);

        return FIXED_BASE_COST + variableCost;
    }
}