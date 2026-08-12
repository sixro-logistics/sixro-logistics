package com.sixro.logistics.hub.route.domain.model;

public enum PathSearchType {
    DISTANCE,  // 최단 거리
    DURATION,  // 최소 시간
    COST,      // 최소 비용
    OPTIMAL    // 최적 (거리 + 시간 + 비용 복합 가중치)
}