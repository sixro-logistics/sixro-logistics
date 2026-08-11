package com.sixro.logistics.user.infrastructure.client;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.user.infrastructure.client.config.AffiliationClientConfig;
import com.sixro.logistics.user.infrastructure.client.response.InternalHubResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "hub-service",
        contextId = "hubAffiliationClient",
        path = "/api/v1/internal/hubs",
        configuration = AffiliationClientConfig.class
)
public interface HubServiceClient {

    @GetMapping("/{hubId}")
    CommonResponse<InternalHubResponse> getHub(
            @PathVariable("hubId") UUID hubId
    );
}