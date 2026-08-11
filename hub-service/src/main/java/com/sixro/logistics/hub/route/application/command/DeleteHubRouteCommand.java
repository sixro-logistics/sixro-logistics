package com.sixro.logistics.hub.route.application.command;

import java.util.UUID;

public record DeleteHubRouteCommand(
        UUID routeId,
        UUID deletedBy
) {}