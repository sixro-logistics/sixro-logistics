package com.sixro.logistics.hub.route.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

/**
 * 다익스트라 탐색망(Route Network) 구성을 Edge 읽기 모델
 * Redis 캐싱 대상
 */
public record RouteNetworkEdge(
        UUID routeId,
        UUID originHubId,
        UUID destinationHubId,
        int distance,
        int duration,
        int baseCost,
        int tollFee
) {
    @JsonIgnore
    public RouteCost getRouteCost() {
        return new RouteCost(baseCost, tollFee);
    }
}