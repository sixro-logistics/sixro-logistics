package com.sixro.logistics.order.application.result;

import java.util.UUID;

public record OrderResultItem(
        UUID productId,
        Integer quantity
) {
}
