package com.sixro.logistics.inventory.application.command;

import java.util.List;
import java.util.UUID;

public record InventoryDeductCommand(
        UUID hubId,
        List<InventoryDeductItem> items
) {
}
