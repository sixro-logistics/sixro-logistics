package com.sixro.logistics.order.application.port;

import java.util.List;
import java.util.UUID;

public interface InventoryCommandPort {

    void deductInventory(UUID hubId, List<InventoryCommandItem> items);

    void restoreInventory(UUID hubId, List<InventoryCommandItem> items);

}
