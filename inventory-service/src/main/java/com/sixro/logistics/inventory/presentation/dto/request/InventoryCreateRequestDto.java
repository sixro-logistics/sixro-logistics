package com.sixro.logistics.inventory.presentation.dto.request;

import com.sixro.logistics.inventory.application.command.InventoryCreateCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InventoryCreateRequestDto(
        @NotNull(message = "허브 id는 필수입니다.")
        UUID hubId,

        @NotNull(message = "상품 id는 필수입니다.")
        UUID productId,

        @NotNull
        @Min(value = 1, message = "재고는 1 이상이어야 합니다.")
        Integer stock
) {

        public InventoryCreateCommand toCommand(){
                return new InventoryCreateCommand(hubId, productId, stock);
        }

}
