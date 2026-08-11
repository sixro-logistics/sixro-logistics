package com.sixro.logistics.hub.hub.presentation.response;

import com.sixro.logistics.hub.hub.application.query.HubInternalInfo;
import java.util.UUID;

public record HubInternalResponse(
        UUID hubId,
        String hubName
) {
    public static HubInternalResponse from(HubInternalInfo info) {
        return new HubInternalResponse(
                info.hubId(),
                info.hubName()
        );
    }
}