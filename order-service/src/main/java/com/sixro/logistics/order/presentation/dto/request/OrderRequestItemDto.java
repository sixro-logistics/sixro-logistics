package com.sixro.logistics.order.presentation.dto.request;

import com.sixro.logistics.order.application.command.OrderCommandItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderRequestItemDto(

        @NotNull(message = "상품 id는 필수입니다.")
        UUID productId,

        @NotNull(message = "상품 수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        Integer quantity
) {

        public OrderCommandItem toCommand() {
                return new OrderCommandItem(productId, quantity);
        }

}
