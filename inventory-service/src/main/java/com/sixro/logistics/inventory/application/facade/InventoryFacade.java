package com.sixro.logistics.inventory.application.facade;

import com.sixro.logistics.inventory.application.model.HubInfo;
import com.sixro.logistics.inventory.application.command.InventoryCreateCommand;
import com.sixro.logistics.inventory.application.model.ProductInfo;
import com.sixro.logistics.inventory.application.port.HubQueryPort;
import com.sixro.logistics.inventory.application.port.ProductQueryPort;
import com.sixro.logistics.inventory.application.result.InventoryCreateResult;
import com.sixro.logistics.inventory.application.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryFacade {

    private final InventoryService inventoryService;
    private final HubQueryPort hubQueryPort;
    private final ProductQueryPort productQueryPort;

    public InventoryCreateResult createInventory(InventoryCreateCommand createCommand) {
        /*HubInfo hub = hubQueryPort.getHub(createCommand.hubId());
        ProductInfo product = productQueryPort.getProduct(createCommand.productId());
        validate(hub, product);*/
        return inventoryService.createInventory(createCommand);
    }

    private void validate(HubInfo hub, ProductInfo product) {
    }
}
