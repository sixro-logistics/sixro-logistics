package com.sixro.logistics.inventory.application.command;

import java.util.List;
import java.util.UUID;

public record InventoryRestoreCommand(
        UUID hubId,
        List<InventoryCommandItem> items
) {
}
