package com.sixro.logistics.inventory.presentation.dto.response;

import com.sixro.logistics.inventory.application.result.InventoryUpdateResult;

import java.util.UUID;

public record InventoryUpdateResponseDto(
        UUID inventoryId,
        Integer stock
) {

    public static InventoryUpdateResponseDto from(InventoryUpdateResult result){
        return new InventoryUpdateResponseDto(
                result.inventoryId(),
                result.stock()
        );
    }

}
