package com.sixro.logistics.order.infrastructure.client.hub;

import java.util.UUID;

public record HubClientResponse(
        UUID hubId
        //HubStatus hubStatus
) {
}