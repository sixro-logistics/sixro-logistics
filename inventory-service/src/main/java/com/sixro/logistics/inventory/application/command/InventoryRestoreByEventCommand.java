package com.sixro.logistics.inventory.application.command;

import java.util.List;
import java.util.UUID;

public record InventoryRestoreByEventCommand(
        UUID eventId,
        UUID orderId,
        UUID hubId,
        List<InventoryCommandItem> items
) {
}
