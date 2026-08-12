package com.sixro.logistics.order.infrastructure.client.product;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "product-service", path = "/api/v1/internal/products")
public interface ProductClient {

    @PostMapping("/{productId}")
    ProductClientResponse getProduct(@PathVariable UUID productId);

    @PostMapping("/check")
    ProductClientListResponse getProducts(@RequestBody List<UUID> productIds);

}