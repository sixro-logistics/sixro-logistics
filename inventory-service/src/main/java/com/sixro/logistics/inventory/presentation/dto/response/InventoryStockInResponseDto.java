package com.sixro.logistics.inventory.presentation.dto.response;

import com.sixro.logistics.inventory.application.result.InventoryStockInCompanyResult;

import java.util.UUID;

public record InventoryStockInResponseDto(
        UUID inventoryId,
        Integer stock
) {

    public static InventoryStockInResponseDto from(InventoryStockInCompanyResult result){
        return new InventoryStockInResponseDto(
                result.inventoryId(),
                result.stock()
        );
    }

}