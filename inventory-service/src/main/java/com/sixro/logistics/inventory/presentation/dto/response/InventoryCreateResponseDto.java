package com.sixro.logistics.inventory.presentation.dto.response;

import com.sixro.logistics.inventory.application.result.InventoryCreateResult;
import java.util.UUID;

public record InventoryCreateResponseDto(
        UUID inventoryId,
        UUID hubId,
        UUID companyId,
        UUID productId,
        Integer stock
) {

    public static InventoryCreateResponseDto from(InventoryCreateResult createResult){
        return new InventoryCreateResponseDto(
                createResult.inventoryId(),
                createResult.hubId(),
                createResult.companyId(),
                createResult.productId(),
                createResult.stock());
    }

}
