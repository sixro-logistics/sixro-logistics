package com.sixro.logistics.order.infrastructure.client.product;

import java.util.List;

public record ProductClientListResponse(
        List<ProductClientResponse> products
) {
}
