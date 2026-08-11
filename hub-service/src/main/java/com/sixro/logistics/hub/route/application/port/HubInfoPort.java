package com.sixro.logistics.hub.route.application.port;

import java.util.List;
import java.util.UUID;

public interface HubInfoPort {
    // 허브 ID로 해당 허브의 위치 좌표(위/경도)를 조회
    HubCoordinate getHubCoordinate(UUID hubId);

    // 전체 허브의 이름과 ID를 매핑
    List<HubBasicInfo> getAllHubs();

    record HubCoordinate(double longitude, double latitude) {}

    record HubBasicInfo(UUID hubId, String hubName, double longitude, double latitude) {}
}