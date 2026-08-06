package com.sixro.logistics.inventory.application.command;

import java.util.UUID;

public record InventoryCreateCommand(
        UUID hubId,
        UUID productId,
        Integer stock
) {
}
