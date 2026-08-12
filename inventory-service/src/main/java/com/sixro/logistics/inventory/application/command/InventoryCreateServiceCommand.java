package com.sixro.logistics.inventory.application.command;

import java.util.UUID;

public record InventoryCreateServiceCommand(
        UUID hubId,
        UUID companyId,
        UUID productId,
        Integer stock
) {
}