package com.sixro.logistics.delivery.infrastructure.client.hubroute;

import com.sixro.logistics.delivery.application.model.HubRouteProductInfo;

import java.util.List;
import java.util.UUID;

public record HubRouteClientRequest(
        List<Product> products
) {
    public static HubRouteClientRequest from(List<HubRouteProductInfo> products) {
        return new HubRouteClientRequest(
                products.stream()
                        .map(product -> new Product(product.productId(), product.quantity()))
                        .toList()
        );
    }

    public record Product(
            UUID productId,
            Integer quantity
    ) {
    }
}
