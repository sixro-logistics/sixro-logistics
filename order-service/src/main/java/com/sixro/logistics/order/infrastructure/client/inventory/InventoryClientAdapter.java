package com.sixro.logistics.order.infrastructure.client.inventory;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.order.application.model.InventoryInfo;
import com.sixro.logistics.order.application.model.InventoryItemInfo;
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
    public InventoryInfo getInventories(UUID hubId, List<UUID> productIds) {

        InventoryClientResponse response =
                inventoryClient.getInventories(
                        new InventoryCheckRequest(hubId, productIds)
                );

        /*CommonResponse<InventoryClientResponse> response =
                inventoryClient.getInventories(
                        new InventoryCheckRequest(hubId, productIds)
                );

        InventoryClientResponse data = response.getData();*/

        return new InventoryInfo(
                response.hubId(),
                response.inventories().stream()
                        .map(item -> new InventoryItemInfo(
                                item.productId(),
                                item.stock()
                        ))
                        .toList()
        );
    }

}
