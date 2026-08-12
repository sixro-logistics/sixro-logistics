package com.sixro.logistics.hub.route.application.port;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface HubInfoPort {
    // 허브 ID로 해당 허브의 위치 좌표(위/경도)를 조회
    HubCoordinate getHubCoordinate(UUID hubId);
    // 전체 허브의 이름과 ID를 매핑 용도
    List<HubBasicInfo> getAllHubs();
    // CLOSED 필터링 용도
    Set<UUID> findClosedHubIds();
    // N+1 방어를 위한 허브 id:이름 목록 조회
    Map<UUID, String> getHubNames(Set<UUID> hubIds);
    // 다익스트라에 혼잡률 반영을 위한 허브별 maxCapacity 조회
    Map<UUID, Integer> getHubCapacities(List<UUID> hubIds);
    // Redis 유실 시 DB 스냅샷(p_hub_metrics) 조회
    Map<UUID, Integer> getHubVolumeSnapshots(List<UUID> hubIds);

    record HubCoordinate(double longitude, double latitude) {}
    record HubBasicInfo(UUID hubId, String hubName, double longitude, double latitude) {}
}