package com.sixro.logistics.delivery.infrastructure.client.hub;

import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service", path = "/api/v1/internal/hubs")
public interface HubClient {

    @GetMapping("/{hubId}")
    CommonResponse<HubClientResponse> getHub(@PathVariable("hubId") UUID hubId);
}
