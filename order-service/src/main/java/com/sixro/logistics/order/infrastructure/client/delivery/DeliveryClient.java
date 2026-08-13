package com.sixro.logistics.order.infrastructure.client.delivery;

import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "delivery-service", path = "/api/v1/internal/deliveries")
public interface DeliveryClient {

    @GetMapping
    CommonResponse<DeliveryClientResponse> getDeliveryManagerIds(@RequestParam UUID orderId);

}
