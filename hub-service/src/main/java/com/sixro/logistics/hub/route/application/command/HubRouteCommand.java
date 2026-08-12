package com.sixro.logistics.hub.route.application.command;

import java.util.UUID;

public class HubRouteCommand {

    public record Create(
            UUID originHubId,
            UUID destinationHubId
    ) {}

    public record Update(
            Integer baseCost
    ) {}

    public record Sync(
            Integer totalValue, // 1: 모든 정보 갱신, 2: 요약 정보만 갱신
            String trafficInfo  // Y: 교통 정보 포함, N: 교통 정보 미포함
    ) {}
}