package com.sixro.logistics.inventory.infrastructure.client.hub;

import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service", path = "/api/v1/internal/hubs")
public interface HubClient {

    @GetMapping("/{hub_id}")
    CommonResponse<HubClientResponse> getHub(@PathVariable UUID hub_id);

}
