package com.sixro.logistics.hub.route.application.port;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RouteMetricsPort {
    // 경로 탐색을 위한 허브별 현재 물동량 조회
    Map<UUID, Integer> getCurrentVolumes(List<UUID> hubIds);
    // Redis 데이터 유실 시 DB 스냅샷을 통한 자가 복구용
    void setVolume(UUID hubId, int volume);
}