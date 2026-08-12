package com.sixro.logistics.hub.route.presentation.response;

import com.sixro.logistics.hub.route.application.query.HubRouteDetailInfo;
import java.util.UUID;

// 단건/상세 응답용 DTO
public record HubRouteDetailResponse(
        UUID hubRouteId,
        UUID originHubId,
        UUID destinationHubId,
        int distance,
        int duration,
        int baseCost,
        int tollFee
) {
    // Info 객체를 받아 Response로 변환하는 정적 팩토리 메서드
    public static HubRouteDetailResponse from(HubRouteDetailInfo info) {
        return new HubRouteDetailResponse(
                info.hubRouteId(),
                info.originHubId(),
                info.destinationHubId(),
                info.distance(),
                info.duration(),
                info.baseCost(),
                info.tollFee()
        );
    }
}