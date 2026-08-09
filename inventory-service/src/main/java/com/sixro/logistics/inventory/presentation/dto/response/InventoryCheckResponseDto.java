package com.sixro.logistics.inventory.presentation.dto.response;

import com.sixro.logistics.inventory.application.result.InventoryCheckResult;

import java.util.List;
import java.util.UUID;

public record InventoryCheckResponseDto(
        UUID hubId,
        List<InventoryResultItem> inventories
) {

    public static InventoryCheckResponseDto from(InventoryCheckResult result){
        return new InventoryCheckResponseDto(
                result.hubId(),
                result.items().stream()
                        .map(item
                                -> new InventoryResultItem(item.productId(), item.stock()))
                        .toList()
                );
    }

}
