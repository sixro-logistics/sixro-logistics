package com.sixro.logistics.order.presentation.dto.response;

import java.util.UUID;

public record OrderItemResponseDto(
        UUID productId,
        Integer quantity
) {
}