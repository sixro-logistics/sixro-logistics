package com.sixro.logistics.hub.route.application.port;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import lombok.Builder;

@Builder
public record RouteSearchCondition(
        double startX,
        double startY,
        double endX,
        double endY,
        int totalValue,
        String trafficInfo
) {
    public RouteSearchCondition {
        // 대한민국 영토 Bounding Box 검증
        if (!isValidLongitude(startX) || !isValidLongitude(endX) ||
                !isValidLatitude(startY) || !isValidLatitude(endY)) {
            throw new BaseException(HubRouteErrorCode.INVALID_LOCATION_BOUNDS);
        }

        totalValue = (totalValue == 0) ? 1 : totalValue;
        trafficInfo = (trafficInfo == null || trafficInfo.isBlank()) ? "N" : trafficInfo;
    }

    private static boolean isValidLongitude(double lon) {
        return lon >= 124.0 && lon <= 132.0;
    }

    private static boolean isValidLatitude(double lat) {
        return lat >= 33.0 && lat <= 39.0;
    }
}