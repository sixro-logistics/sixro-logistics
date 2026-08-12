package com.sixro.logistics.order.infrastructure.client.product;

import java.util.UUID;

public record ProductClientResponse(
        UUID productId,
        String productName,
        Integer price,
        UUID companyId
) {
}
