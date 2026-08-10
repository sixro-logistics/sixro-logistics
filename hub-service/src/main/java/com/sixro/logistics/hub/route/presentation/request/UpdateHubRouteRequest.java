package com.sixro.logistics.hub.route.presentation.request;

import com.sixro.logistics.hub.route.application.command.UpdateHubRouteCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateHubRouteRequest(
        @NotNull(message = "기본 운임은 필수입니다.")
        @Min(value = 0, message = "기본 운임은 0 이상이어야 합니다.")
        Integer baseCost
) {
    public UpdateHubRouteCommand toCommand() {
        return new UpdateHubRouteCommand(baseCost);
    }
}