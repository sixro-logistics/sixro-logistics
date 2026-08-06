package com.sixro.logistics.inventory.application.facade;

import com.sixro.logistics.inventory.application.command.InventoryCreateCommand;
import com.sixro.logistics.inventory.application.result.InventoryCreateResult;
import com.sixro.logistics.inventory.application.service.InventoryService;
import com.sixro.logistics.inventory.infrastructure.client.hub.HubClient;
import com.sixro.logistics.inventory.infrastructure.client.hub.HubClientResponse;
import com.sixro.logistics.inventory.infrastructure.client.product.ProductClient;
import com.sixro.logistics.inventory.infrastructure.client.product.ProductClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryFacade {

    private final InventoryService inventoryService;
    private final HubClient hubClient;
    private final ProductClient productClient;

    public InventoryCreateResult createInventory(InventoryCreateCommand createCommand) {
        /*
        HubClientResponse hub = hubClient.getHub(requestDto.hubId());
        ProductClientResponse product = productClient.getProduct(requestDto.productId());
        validateAuthority(hub, product);
        */
        return inventoryService.createInventory(createCommand);
    }

    private void validateAuthority(HubClientResponse hub, ProductClientResponse product) {
    }
}
