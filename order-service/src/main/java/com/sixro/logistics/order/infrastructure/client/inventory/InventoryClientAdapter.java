package com.sixro.logistics.order.infrastructure.client.inventory;

import com.sixro.logistics.order.application.model.InventoryInfo;
import com.sixro.logistics.order.application.port.InventoryCheckItem;
import com.sixro.logistics.order.application.port.InventoryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryClientAdapter implements InventoryQueryPort {

    private final InventoryClient inventoryClient;

    @Override
    public List<InventoryInfo> getInventories(UUID hubId, List<UUID> productIds) {

        InventoryClientResponse response =
                inventoryClient.getInventories(
                        new InventoryCheckRequest(hubId, productIds)
                );

        return response.inventories().stream()
                .map(inventory -> new InventoryInfo(
                        inventory.hubId(),
                        inventory.productId(),
                        inventory.stock()
                ))
                .toList();
    }

}
