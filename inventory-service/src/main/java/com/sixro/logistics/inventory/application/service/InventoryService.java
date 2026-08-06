package com.sixro.logistics.inventory.application.service;

import com.sixro.logistics.inventory.application.command.InventoryCreateCommand;
import com.sixro.logistics.inventory.application.result.InventoryCreateResult;
import com.sixro.logistics.inventory.domain.entity.Inventory;
import com.sixro.logistics.inventory.domain.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public InventoryCreateResult createInventory(InventoryCreateCommand createCommand) {

        Inventory inventory = Inventory.create(
                createCommand.hubId(),
                createCommand.productId(),
                createCommand.stock()
        );

        Inventory createdInventory = inventoryRepository.save(inventory);
        return new InventoryCreateResult(
                createdInventory.getId(),
                createdInventory.getHubId(),
                createdInventory.getProductId(),
                createdInventory.getStock()
        );
    }

}
