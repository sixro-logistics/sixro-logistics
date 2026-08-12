package com.sixro.logistics.hub.route.domain.model;

import java.util.UUID;

public record HubTransferMetric(
        UUID hubId,
        int baseTransferTimeSeconds, // 기본 환적시간 (상하차 소요 시간)
        double congestionRate        // 혼잡률
) {
    public int getExpectedTransferTime() {
        return (int) (baseTransferTimeSeconds * congestionRate);
    }

    // 혼잡률 계산 팩토리 메서드
    public static HubTransferMetric of(UUID hubId, int baseTransferTimeSeconds, int currentVolume, int maxCapacity) {
        if (maxCapacity <= 0) maxCapacity = 10000; // 0으로 나누기 방지, maxCapacity 의 최소값 활용

        double utilization = (double) currentVolume / maxCapacity; // 적재율 = 물동량 / 최대 처리 용량
        double rate = 1.0 + Math.pow(Math.max(0, utilization - 0.5) * 2, 3); // 혼잡률

        return new HubTransferMetric(hubId, baseTransferTimeSeconds, rate);
    }
}