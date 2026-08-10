package com.sixro.logistics.hub.route.infrastructure.client.tmap;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface TmapRouteClient {

    String DEFAULT_VERSION = "1";  // 2026.08 기준 version1 만 지원

    /**
     * TMAP 자동차 경로 안내 API
     */
    @PostExchange("/routes")
    TmapRouteResponse getRouteInfo(
            @RequestParam("version") String version,
            @RequestBody TmapRouteRequest request
    );
}