package com.sixro.logistics.delivery.application.port;

import com.sixro.logistics.delivery.application.model.HubRoutePathInfo;
import com.sixro.logistics.delivery.application.model.HubRouteProductInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// app -> infra 요구 기능 정의
public interface HubRouteQueryPort {

    Optional<HubRoutePathInfo> findPath(UUID originHubId, UUID destHubId, List<HubRouteProductInfo> products);
}
