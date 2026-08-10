package com.sixro.logistics.order.infrastructure.client.product;

import java.util.List;

public record ProductClientResponse(
        List<ProductClientProduct> products
) {
}
