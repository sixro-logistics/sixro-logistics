package com.sixro.logistics.delivery.infrastructure.client.hub;

import java.util.UUID;

public record HubClientResponse(
        UUID hubId,
        String hubName
) {
}
