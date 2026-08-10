package com.sixro.logistics.order.application.port;

import com.sixro.logistics.order.application.model.InventoryInfo;

import java.util.List;
import java.util.UUID;

public interface InventoryQueryPort {

    InventoryInfo getInventories(UUID hubId, List<UUID> productIds);

}
