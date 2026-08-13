package com.sixro.logistics.order.infrastructure.client.user;

import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/v1/internal/users")
public interface UserClient {

    @GetMapping("/{userId}/status")
    CommonResponse<UserClientResponse> getUser(@PathVariable UUID userId);

}
