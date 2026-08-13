package com.sixro.logistics.hub.hub.application.port;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HubMetricsPort {
    int increaseVolume(UUID hubId, int increaseAmount);
    int decreaseVolume(UUID hubId, int decreaseAmount);
    Map<UUID, Integer> getCurrentVolumes(List<UUID> hubIds);
}