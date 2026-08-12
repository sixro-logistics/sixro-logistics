package com.sixro.logistics.inventory.presentation.dto.response;

import com.sixro.logistics.inventory.application.result.InventorySearchItem;
import java.util.UUID;

public record InventorySearchResponseDto(
        UUID inventoryId,
        UUID hubId,
        UUID companyId,
        UUID productId,
        Integer stock
) {

    public static InventorySearchResponseDto from(
            InventorySearchItem item
    ) {
        return new InventorySearchResponseDto(
                item.inventoryId(),
                item.hubId(),
                item.companyId(),
                item.productId(),
                item.stock()
        );
    }

}
