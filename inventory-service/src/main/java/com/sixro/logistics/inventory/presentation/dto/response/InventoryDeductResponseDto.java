package com.sixro.logistics.inventory.presentation.dto.response;

import com.sixro.logistics.inventory.application.result.InventoryDeductResult;

import java.util.List;
import java.util.UUID;

public record InventoryDeductResponseDto(
        UUID hubId,
        List<InventoryResponseItem> inventories
) {

    public static InventoryDeductResponseDto from(InventoryDeductResult result){
        return new InventoryDeductResponseDto(
                result.hubId(),
                result.items().stream()
                        .map(item
                                -> new InventoryResponseItem(item.productId(), item.stock()))
                        .toList()
        );
    }

}