package com.sixro.logistics.hub.route.presentation.request;

import com.sixro.logistics.hub.route.application.command.CreateHubRouteCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateHubRouteRequest(
        @NotNull(message = "출발 허브 ID는 필수입니다.")
        UUID originHubId,

        @NotNull(message = "도착 허브 ID는 필수입니다.")
        UUID destinationHubId
) {
    public CreateHubRouteCommand toCommand() {
        return new CreateHubRouteCommand(originHubId, destinationHubId);
    }
}