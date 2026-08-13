package com.sixro.logistics.hub.hub.presentation.dto;

import java.util.UUID;

public record MockVolumeRequest(
        UUID hubId,
        int totalVolume
) {
}