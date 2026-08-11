package com.sixro.logistics.inventory.presentation.dto.response;

import com.sixro.logistics.inventory.application.result.InventoryDeleteResult;

import java.util.UUID;

public record InventoryDeleteResponseDto(
        UUID inventoryId
) {

    public static InventoryDeleteResponseDto from(InventoryDeleteResult result) {
        return new InventoryDeleteResponseDto(result.inventoryId());
    }

}
