package com.sixro.logistics.hub.route.application.command;

import java.util.UUID;

public record CreateHubRouteCommand(
        UUID originHubId,
        UUID destinationHubId
) {}
