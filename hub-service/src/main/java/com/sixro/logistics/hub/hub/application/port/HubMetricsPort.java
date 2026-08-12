package com.sixro.logistics.hub.hub.application.port;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HubMetricsPort {
    void increaseVolume(UUID hubId, int boxCount);
    int decreaseVolume(UUID hubId, int maxDeductAmount);
    Map<UUID, Integer> getCurrentVolumes(List<UUID> hubIds);
}