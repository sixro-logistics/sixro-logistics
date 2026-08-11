package com.sixro.logistics.inventory.application.command;

import java.util.UUID;

public record InventorySearchCommand(
        UUID hubId,
        UUID companyId,
        UUID productId
) {
}
