package com.sixro.logistics.inventory.application.command;

import java.util.UUID;

public record InventoryDeductItem(
        UUID productId,
        Integer quantity
) {
}
