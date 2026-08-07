package com.sixro.logistics.order.infrastructure.client.product;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "product-service", path = "/api/v1")
public interface ProductClient {

    @PostMapping("/products/check")
    ProductClientResponse getProducts(@RequestBody List<UUID> productIds);

}