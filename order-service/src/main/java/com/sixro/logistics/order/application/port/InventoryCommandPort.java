package com.sixro.logistics.order.application.port;

import java.util.List;
import java.util.UUID;

public interface InventoryCommandPort {

    void deductInventory(
            UUID idempotencyKey,
            UUID hubId,
            List<InventoryCommandItem> items
    );

    void restoreInventory(
            UUID idempotencyKey,
            UUID hubId,
            List<InventoryCommandItem> items
    );

}
