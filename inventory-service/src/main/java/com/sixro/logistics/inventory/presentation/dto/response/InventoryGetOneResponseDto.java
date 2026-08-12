package com.sixro.logistics.inventory.presentation.dto.response;

import com.sixro.logistics.inventory.application.result.InventoryGetOneResult;

import java.util.UUID;

public record InventoryGetOneResponseDto(
        UUID inventoryId,
        UUID hubId,
        UUID companyId,
        UUID productId,
        Integer stock
) {

    public static InventoryGetOneResponseDto from(InventoryGetOneResult result){
        return new InventoryGetOneResponseDto(
                result.inventoryId(),
                result.hubId(),
                result.companyId(),
                result.productId(),
                result.stock()
        );
    }

}
