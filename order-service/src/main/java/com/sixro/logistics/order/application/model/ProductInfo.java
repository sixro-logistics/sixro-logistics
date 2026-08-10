package com.sixro.logistics.order.application.model;

import java.util.UUID;

public record ProductInfo(
        UUID productId,
        String productName,
        Integer price,
        UUID companyId
) {
}