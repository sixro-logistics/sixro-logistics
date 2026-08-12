package com.sixro.logistics.order.infrastructure.client.hub;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service", path = "/api/v1/internal/hubs")
public interface HubClient {

    @GetMapping("/{hubId}")
    HubClientResponse getHub(@PathVariable UUID hubId);

}
