package com.sixro.logistics.order.application.result;

import java.util.UUID;

public record OrderResultItem(
        UUID productId,
        String productName,
        Integer productPrice,
        Integer quantity,
        UUID companyId
) {
}
