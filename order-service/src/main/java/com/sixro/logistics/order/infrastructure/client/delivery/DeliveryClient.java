package com.sixro.logistics.order.infrastructure.client.delivery;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "delivery-service", path = "/api/v1/internal/deliveries")
public interface DeliveryClient {

    @GetMapping
    DeliveryClientResponse getDeliveryManagerIds(@RequestParam UUID orderId);

}
