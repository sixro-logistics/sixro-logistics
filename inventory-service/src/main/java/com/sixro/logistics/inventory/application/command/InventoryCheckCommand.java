package com.sixro.logistics.inventory.application.command;

import java.util.List;
import java.util.UUID;

public record InventoryCheckCommand(
        UUID hubId,
        List<UUID> productIds
) {
}
