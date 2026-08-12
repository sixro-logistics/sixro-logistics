package com.sixro.logistics.inventory.application.result;

import java.util.UUID;

public record InventoryStockInCompanyResult(
        UUID inventoryId,
        Integer stock
) {

}