package com.sixro.logistics.order.presentation.dto.response;

import java.util.UUID;

public record OrderResponseItem(
        UUID productId,
        String productName,
        Integer productPrice,
        Integer quantity,
        UUID companyId
) {
}