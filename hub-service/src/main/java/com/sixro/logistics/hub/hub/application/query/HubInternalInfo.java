package com.sixro.logistics.hub.hub.application.query;

import java.util.UUID;

public record HubInternalInfo(
        UUID hubId,
        String hubName
) {}