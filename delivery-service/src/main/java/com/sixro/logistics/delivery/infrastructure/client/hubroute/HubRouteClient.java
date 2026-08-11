package com.sixro.logistics.delivery.infrastructure.client.hubroute;

import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "hub-service", contextId = "hubRouteClient", path = "/api/v1/internal/hub-routes")
public interface HubRouteClient {

    @PostMapping("/paths")
    CommonResponse<HubRouteClientResponse> getPath(
            @RequestParam("searchType") String searchType,
            @RequestBody HubRouteClientRequest request
    );
}
