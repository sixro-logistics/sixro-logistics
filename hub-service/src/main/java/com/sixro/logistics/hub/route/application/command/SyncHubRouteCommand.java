package com.sixro.logistics.hub.route.application.command;

public record SyncHubRouteCommand(
        Integer totalValue,
        String trafficInfo
) {}
