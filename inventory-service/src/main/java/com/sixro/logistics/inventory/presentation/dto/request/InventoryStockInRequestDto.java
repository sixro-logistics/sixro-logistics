package com.sixro.logistics.inventory.presentation.dto.request;

import com.sixro.logistics.inventory.application.command.InventoryStockInCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryStockInRequestDto(
        @NotNull
        @Min(value = 0, message = "수량은 1 이상이어야 합니다.")
        Integer quantity
) {

    public InventoryStockInCommand toCommand(){
        return new InventoryStockInCommand(quantity);
    }

}
