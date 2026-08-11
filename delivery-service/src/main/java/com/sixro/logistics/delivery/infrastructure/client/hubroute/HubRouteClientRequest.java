package com.sixro.logistics.delivery.infrastructure.client.hubroute;

import com.sixro.logistics.delivery.application.model.HubRouteProductInfo;

import java.util.List;
import java.util.UUID;

public record HubRouteClientRequest(
        UUID originHubId,
        UUID destHubId,
        List<Product> products
) {
    public static HubRouteClientRequest from(
            UUID originHubId,
            UUID destHubId,
            List<HubRouteProductInfo> products
    ) {
        return new HubRouteClientRequest(
                originHubId,
                destHubId,
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
