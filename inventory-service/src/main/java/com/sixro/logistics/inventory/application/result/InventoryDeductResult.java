package com.sixro.logistics.inventory.application.result;

import java.util.List;
import java.util.UUID;

public record InventoryDeductResult(
        UUID hubId,
        List<InventoryResultItem> items
) {
}
