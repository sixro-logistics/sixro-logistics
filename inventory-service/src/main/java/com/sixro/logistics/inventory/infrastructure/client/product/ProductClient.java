package com.sixro.logistics.inventory.infrastructure.client.product;

import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service", path = "/api/v1/internal/products")
public interface ProductClient {

    @GetMapping("/{productId}")
    CommonResponse<ProductClientResponse> getProduct(@PathVariable UUID productId);

}
