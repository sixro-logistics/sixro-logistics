package com.sixro.logistics.hub.route.presentation.request;

import com.sixro.logistics.hub.route.application.command.SyncHubRouteCommand;

public record SyncHubRouteRequest(
        Integer totalValue,
        String trafficInfo
) {
    public SyncHubRouteCommand toCommand() {

        int safeTotalValue = (totalValue != null && totalValue > 0) ? totalValue : 1;
        String safeTrafficInfo = (trafficInfo != null && !trafficInfo.isBlank()) ? trafficInfo : "N";

        return new SyncHubRouteCommand(safeTotalValue, safeTrafficInfo);
    }
}