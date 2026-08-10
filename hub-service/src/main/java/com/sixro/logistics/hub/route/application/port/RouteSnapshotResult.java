package com.sixro.logistics.hub.route.application.port;

import lombok.Builder;
import org.locationtech.jts.geom.LineString;

@Builder
public record RouteSnapshotResult(
        int distance,           // 이동 거리 (m)
        int duration,           // 소요 시간 (초)
        int tollFee,            // 통행 요금 (원)
        LineString routePath    // 경로 지리적 형상 (PostGIS 호환)
) {}