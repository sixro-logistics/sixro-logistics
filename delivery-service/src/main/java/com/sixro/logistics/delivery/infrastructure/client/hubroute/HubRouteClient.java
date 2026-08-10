package com.sixro.logistics.delivery.infrastructure.client.hubroute;

import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "hub-service", contextId = "hubRouteClient", path = "/api/v1/internal/hub-routes")
public interface HubRouteClient {

    @PostMapping("/path")
    CommonResponse<HubRouteClientResponse> getPath(
            @RequestParam("originHubId") UUID originHubId,
            @RequestParam("destHubId") UUID destHubId,
            @RequestBody HubRouteClientRequest request
    );
}
