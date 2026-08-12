package com.sixro.logistics.inventory.application.command;

import java.util.UUID;

public record InventoryCommandItem(
        UUID productId,
        Integer quantity
) {
}
