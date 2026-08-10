package com.sixro.logistics.inventory.infrastructure.client.product;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service", path = "/api/v1")
public interface ProductClient {

    @GetMapping("/products/{productId}")
    ProductClientResponse getProduct(@PathVariable UUID productId);

}
