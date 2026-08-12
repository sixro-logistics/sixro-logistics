package com.sixro.logistics.hub.hub.domain.event;

import com.sixro.logistics.hub.hub.domain.model.HubStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubStatusChangedEvent(
        UUID hubId,
        HubStatus previousStatus,
        HubStatus newStatus,
        LocalDateTime occurredAt
) {
    public static HubStatusChangedEvent of(UUID hubId, HubStatus previousStatus, HubStatus newStatus) {
        return new HubStatusChangedEvent(hubId, previousStatus, newStatus, LocalDateTime.now());
    }
}