package com.sixro.logistics.inventory.presentation.dto.request;

import com.sixro.logistics.inventory.application.command.InventoryUpdateCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryUpdateRequestDto(
        @NotNull
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        Integer stock
) {

    public InventoryUpdateCommand toCommand(){
        return new InventoryUpdateCommand(stock);
    }

}
