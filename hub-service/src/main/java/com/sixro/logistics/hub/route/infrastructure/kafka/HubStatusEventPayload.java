package com.sixro.logistics.hub.route.infrastructure.kafka;

import java.util.UUID;

public record HubStatusEventPayload(
        UUID hubId,
        String previousStatus,
        String newStatus
) {}