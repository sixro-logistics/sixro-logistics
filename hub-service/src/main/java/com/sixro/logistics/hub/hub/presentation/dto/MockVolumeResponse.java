package com.sixro.logistics.hub.hub.presentation.dto;

import java.util.UUID;

public record MockVolumeResponse(
        UUID eventId,
        UUID hubId,
        int injectedVolume,
        String message
) {
}