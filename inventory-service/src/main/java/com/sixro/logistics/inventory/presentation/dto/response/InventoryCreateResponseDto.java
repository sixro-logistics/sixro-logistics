package com.sixro.logistics.inventory.presentation.dto.response;

import com.sixro.logistics.inventory.domain.entity.Inventory;

import java.util.UUID;

public record InventoryCreateResponseDto(
        UUID inventoryId,
        UUID hubId,
        UUID productId,
        Integer stock
) {

    public static InventoryCreateResponseDto from(Inventory inventory){
        return new InventoryCreateResponseDto(inventory.getId(),
                inventory.getHubId(),
                inventory.getProductId(),
                inventory.getStock());
    }

}
